default[:histori][:run_as] = 'histori'
default[:histori][:current] = '/home/histori/current/histori-server'
default[:histori][:service_name] = 'histori-api-server.HistoriServer' # don't change this
default[:histori][:dbuser] = 'histori'
default[:histori][:master_db] = 'histori_master'
default[:histori][:db_prefix] = 'histori_'
default[:histori][:java_opts] = '-Xmx1800m -Xms600m'