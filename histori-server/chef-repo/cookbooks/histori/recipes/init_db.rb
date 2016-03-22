#
# Cookbook Name:: histori
# Recipe:: init_db
#
require 'tempfile'

def setup_db (chef, dbname, dbuser, dbpass)
  pgsql = Chef::Recipe::Postgresql
  base = Chef::Recipe::Base
  unless pgsql.table_exists dbname, dbuser, dbpass, 'nexus'
    pgsql.create_db chef, dbname
    pgsql.grant_all chef, dbname, dbuser
    pgsql.initialize_db chef, "#{base.chef_files('histori')}/assets/schema.sql", dbuser, dbpass, dbname
    pgsql.initialize_db chef, "#{base.chef_files('histori')}/assets/index.sql", dbuser, dbpass, dbname
  end
end

pgsql = Chef::Recipe::Postgresql
base = Chef::Recipe::Base

run_as=node[:histori][:run_as]
current=node[:histori][:current]
dbuser=node[:histori][:dbuser]
master_db=node[:histori][:master_db]
db_prefix=node[:histori][:db_prefix]

histori_bag = data_bag_item('histori', 'init')
env = histori_bag['environment']
dbpass = env['HISTORI_DB_PASS']
db_count = histori_bag['db_count']

pgsql.create_user self, dbuser, env['HISTORI_DB_PASS']

# use with extreme caution
if histori_bag['reinit_database'] && histori_bag['reinit_database'] == 'true'
  raise 'reinit_database was set but yes_really_reinit_database was not set' unless histori_bag['yes_really_reinit_database']
  service_name = node[:histori][:service_name]
  bash 'drop all databases' do
    user 'root'
    code <<-EOH
service #{service_name} stop
start=$(date +%s)
TIMEOUT=60
while [ $(ps auxw | grep java | grep HistoriServer | wc -l | tr -d ' ') -gt 0 ] ; do
  if [ $(eval $(date +%s) - ${start}) -gt ${TIMEOUT} ] ; then
    echo "timed out waiting for #{service_name} to stop"
    exit 2
  fi
  echo "waiting for service #{service_name} to stop"
  sleep 2s
done
sudo -u postgres #{current}/scripts/dropalldb #{db_prefix}
EOH
  end
  base.flush_redis self
end

setup_db self, master_db, dbuser, dbpass
for i in 0..(db_count-1) do
  setup_db self, "#{db_prefix}#{i}", dbuser, dbpass
end

# setup shards
bash 'setup shards' do
    user run_as
    cwd current
    code <<-EOH
temp=$(mktemp /tmp/shards.XXXXXXX.sql) || exit 1

# todo: it would be nice to have these shard names programmatically provided, as opposed to hard-coded here
./run.sh shard-gen-sql \
  -S "histori-account, nexus, nexus-archive, super-nexus, vote, vote-archive, bookmark, permalink, tag" \
  -J jdbc:postgresql://127.0.0.1:5432/histori_ \
  -C #{db_count} \
  -M jdbc:postgresql://127.0.0.1:5432/histori_master \
  -o ${temp}
rval=$?
if [ ${rval} -ne 0 ] ; then
  rm -f ${temp}
  exit 1
fi

cat ${temp} | PGPASSWORD=#{dbpass} psql -U #{dbuser} -h 127.0.0.1 #{master_db}
rval=$?
rm -f ${temp}
exit ${rval}

EOH
  not_if { %x(echo "SELECT count(*) FROM shard" | PGPASSWORD=#{dbpass} psql -qt -U #{dbuser} #{master_db} | head -1 | tr -d ' ').strip.to_i > 0 }
end
