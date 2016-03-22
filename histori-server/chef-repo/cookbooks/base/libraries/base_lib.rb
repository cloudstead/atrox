require 'securerandom'
require 'json'

# add String.to_bool
class String
  def to_bool
    return true   if self == true   || self =~ (/(true|t|yes|y|1)$/i)
    return false  if self == false  || self.blank? || self =~ (/(false|f|no|n|0)$/i)
    raise ArgumentError.new("invalid value for Boolean: \"#{self}\"")
  end
  def self.to_bool (s)
    begin
      return s.nil? ? false : s.to_bool
    rescue => e
      puts "to_bool: cannot convert (#{s}), returning false"
      false
    end
  end
end

class Chef::Recipe::Base

  @CLIENT_SECRET = '/etc/.cloudos'

  def self.chef_user
    %x(sudo cat /etc/chef-user).strip
  end

  def self.user_home(user)
    %x(echo $(bash -c "cd ~#{user} && pwd")).strip
  end

  def self.chef_dir
    File.absolute_path("#{File.dirname(__FILE__)}/../../..")
  end

  def self.chef_files(cookbook)
    "#{chef_dir}/cookbooks/#{cookbook}/files/default"
  end

  def self.chef_templates(cookbook)
    "#{chef_dir}/cookbooks/#{cookbook}/templates/default"
  end

  def self.chef_databags(cookbook)
    "#{chef_dir}/data_bags/#{cookbook}"
  end

  def self.chef_datafiles(cookbook)
    "#{chef_dir}/data_files/#{cookbook}"
  end

  def self.chef_user_home
    user_home(chef_user)
  end

  def self.primary_port(chef, app)
    begin
      chef.data_bag_item(app, 'ports')['primary']
    rescue => e
      puts "No ports databag found for #{app}, returning nil"
      nil
    end
  end

  def self.ports_databag(chef, app)
    ports = nil
    begin
      ports = chef.data_bag_item(app, 'ports')
    rescue => e
      puts "No ports databag found for #{app}, creating one"
      ports = { 'id' => 'ports', 'primary' => 'pick', 'admin' => 'pick' }
    end

    writeback = false
    if ports['primary'] == 'pick'
      ports['primary'] = pick_port
      writeback = true
    end
    if ports['admin'] == 'pick'
      ports['admin'] = pick_port
      writeback = true
    end

    if writeback
      databags = chef_databags(app)
      file = "#{databags}/ports.json"
      Dir.mkdir(databags) unless File.exist? databags
      File.open(file, 'w') {|f| f.write(ports.to_json) }
      %x(chown -R #{chef_user} #{databags} && chmod 600 #{file})
    end

    # Now this had better work...
    chef.data_bag_item(app, 'ports')
  end

  # Note: this runs at compile time, so the ports picked can be written to databags referenced at chef-time
  def self.pick_port
    port = 5000 + rand(26000)
    until port_available(port)
      port += 1
      break if port > 32768
    end
    raise 'Could not find an open port!' if port > 32768
    port
  end

  # Note: this runs at compile time, so the ports picked can be written to databags referenced at chef-time
  def self.port_available(port)
    raise 'port_available: port was nil or empty' if port.to_s.empty?
    raise "port_available: invalid port: #{port}" unless port.to_s =~ /^\d+$/

    port_check_output=%x(#{chef_files('base')}/port-available #{port})
    return false unless port_check_output.strip.include? 'available'

    %w(primary admin).each do |type|
      assigned=%x(find #{chef_dir}/data_bags -type f -name ports.json | xargs egrep '"#{type}"[[:space:]]*:[[:space:]]*#{port}')
      return false unless assigned.strip.empty?
    end

    true
  end

  # Note: this method runs at chef-time, because it is used in validating apps are running
  # whereas port_available runs at compile-time, because it is used to generate ports that will used at chef-time
  def self.restart_unless_port(chef, port, service)
    raise 'restart_unless_port: port was nil or empty' if port.to_s.empty?
    raise "restart_unless_port: invalid port: #{port}" unless port.to_s =~ /^\d+$/
    chef.bash "checking port #{port} at #{Time.now} and restarting #{service} if not bound" do
      user 'root'
      cwd '/tmp'
      code <<-EOF
port=#{port}
for addr in $(ifconfig | grep 'inet addr' | tr ':' ' ' | awk '{print $3}') ; do
  if nc -z ${addr} ${port} ; then
    echo "listening on ${addr}"
    exit 0
  fi
done
echo "Nothing was not listening on #{port}, restarting #{service}"
service #{service} restart
sleep 5s
EOF
    end
  end

  def self.secret
    if ! File.exist? @CLIENT_SECRET
      File.open(@CLIENT_SECRET, 'wb') { |f| f.write SecureRandom.hex(64) }
    end
    %x(chmod 700 #{@CLIENT_SECRET})

    file = File.open(@CLIENT_SECRET, 'rb')
    contents = file.read
    file.close

    contents
  end

  def self.password(service)
    Digest::SHA256.hexdigest("#{service}_#{secret}")
  end

  # Returns a random hexadecimal string of arbitrary size.
  def self.random_password(size=8)
    SecureRandom.hex[0..size]
  end

  def self.sha_file(file)
    Digest::SHA256.file(file).hexdigest
  end

  def self.logrotate(chef, app_name, path)
    chef.template "/etc/logrotate.d/#{app_name}" do
      source 'log_rotate.erb'
      owner 'root'
      group 'root'
      mode '0750'
      cookbook 'base'
      variables({ :path => path })
      action :create
    end
  end

  def self.gid(name)
    %x(cat /etc/group | grep "^#{name}:" | awk -F ':' '{print $(NF-1)}').strip
  end

  def self.uid(name)
    %x(id -u #{name}).strip
  end

  def self.default_ssl_cert (chef)
    begin
      base_bag = chef.data_bag_item('base', 'base')
    rescue => e
      puts "default_ssl_cert: No base/base.json databag found (#{e}), returning false"
      return nil
    end
    cert_name = base_bag['ssl_cert_name']
    return nil if cert_name.to_s.empty?

    ssl_pem_src = local_pem_path('base', cert_name)
    ssl_key_src = "#{chef_user_home}/chef/certs/base/#{cert_name}.key"
    (File.exist?(ssl_pem_src) && File.exist?(ssl_key_src)) ? cert_name : nil
  end

  def self.install_ssl_cert(chef, app, name)
    # This is where deploy.sh puts the ssl certs provided in INIT_FILES directory
    ssl_pem_src = local_pem_path(app, name)
    ssl_key_src = "#{chef_user_home}/chef/certs/#{app}/#{name}.key"
    if File.exist?(ssl_pem_src) && File.exists?(ssl_key_src)

      ssl_pem_dest = "/etc/ssl/certs/#{name}.pem"
      ssl_key_dest = "/etc/ssl/private/#{name}.key"

      chef.bash "install SSL cert '#{name}' from #{ssl_pem_src}" do
        user 'root'
        cwd '/tmp'
        code <<-EOF
# use -cv so they are only copied if the checksum differs
rsync -cv #{ssl_pem_src} #{ssl_pem_dest} && chmod 644 #{ssl_pem_dest} && \
rsync -cv #{ssl_key_src} #{ssl_key_dest} && chmod 600 #{ssl_key_dest}
EOF
        not_if { File.exist?(ssl_pem_dest) && File.exists?(ssl_key_dest) }
      end
    end
  end

  def self.local_pem_exists(app, name)
    File.exist? local_pem_path(app, name)
  end

  def self.local_pem_app_path (app)
    "#{chef_user_home}/chef/certs/#{app}"
  end

  def self.local_pem_path(app, name)
    "#{local_pem_app_path(app)}/#{name}.pem"
  end

  def self.local_certs(app)
    %x(for pem in $(find #{local_pem_app_path(app)} -type f -name "*.pem") ; do basename "${pem}" | sed -e 's/.pem$//' ; done).split
  end

  def self.local_pem_cn(app, name)
    pem_cn_path local_pem_path(app, name)
  end

  def self.pem_path(name)
    "/etc/ssl/certs/#{name}.pem"
  end

  def self.pem_cn(name)
    pem_cn_path pem_path(name)
  end

  def self.pem_cn_path(path)
    return nil if path.nil? || !File.exist?(path)

    pem_subject = %x(openssl x509 -in #{path} -subject | grep subject= | head -n 1).strip
    %x(echo 'For path #{path}, subject=#{pem_subject}' > /tmp/pem_cn_#{File.basename(path, '.pem')})
    matches = /\/CN=([^\/]+)\//.match(pem_subject)
    matches ? matches[1] : nil
  end

  def self.set_hostname(chef, fqdn)
    return if fqdn == 'localhost' || is_docker
    chef.bash "set hostname to #{fqdn}" do
      user 'root'
      cwd '/tmp'
      code <<-EOF
if [ $(hostname) != "#{fqdn}" ] ; then
  hostname #{fqdn}
  echo #{fqdn} > /etc/hostname
fi
EOF
    end

    # chef.template '/etc/hosts' do
    #   owner 'root'
    #   group 'root'
    #   mode '0644'
    #   cookbook 'base'
    #   variables({ :fqdn => fqdn })
    #   action :create
    # end
  end

  def self.set_perms (chef, path, owner, perms)
    chef.bash "set_perms: owner=#{owner}, perms=#{perms} for path=#{path}" do
      user 'root'
      code <<-EOF
chown #{owner} #{path} || exit 1
chmod #{perms} #{path} || exit 1
EOF
    end
  end

  def self.remove_from_file (chef, file, data)
    chef.bash "remove #{data} from #{file}" do
      user 'root'
      cwd '/tmp'
      code <<-EOF
found=$(cat #{file} | grep -- "#{data}" | wc -l | tr -d ' ')
if [ $found ] ; then
  temp=$(mktemp /tmp/remove_from_file.XXXXXXX)
  cat #{file} | grep -v -- "#{data}" > ${temp}
  if [[ $(stat --printf="%s" ${temp} -gt 0 ]] ; then
    cp #{file} $(mktemp $(dirname #{file})/$(basename #{file}).$(date +%Y-%m-%d_%H-%M-%S).XXXXX) && \
    cat ${temp} > #{file} && rm -f ${temp}
  fi
fi
EOF
      not_if { File.readlines(file).grep(/Regexp.escape(data)/).empty? }
    end
  end

  def self.user_exists (user)
    %x(sudo -u #{user} -H pwd ; echo $?).strip.to_i == 0
  end

  def self.sysctl (setting, min_value)
    chef.bash "ensure sysctl.#{setting} is at least #{min_value}" do
      user 'root'
      code <<-EOF
sysctl -w #{setting}=#{min_value}
echo "
#{setting}=#{min_value}
" >> /etc/sysctl.conf
EOF
      only_if { %x(sysctl #{setting} | awk -F '=' '{print $2}').to_i < min_value }
    end
  end

  def self.restart_redis (chef)
    chef.bash "restarting redis at #{Time.now}" do
      user 'root'
      code <<-EOF
service redis-server restart
      EOF
    end
  end

  def self.flush_redis (chef)
    chef.bash "flushing redis at #{Time.now}" do
      user 'root'
      code <<-EOF
echo flushall | nc 127.0.0.1 6379
      EOF
    end
  end

  def self.bcrypt (password, rounds = nil)
    bcrypt_script='/usr/bin/bcrypt-password'
    init_bcrypt unless File.exist?(bcrypt_script)
    rounds ||= 10
    %x(echo '#{password}' | #{bcrypt_script} #{rounds}).strip
  end

  def self.init_bcrypt()
    bcrypt_script='/usr/bin/bcrypt-password'
    lib = self
    %x(
      apt-get install -y make unzip gcc &&
      yes | cpan App::cpanminus && cpanm --notest Crypt::Random && cpanm --notest Crypt::Eksblowfish &&
      cp #{lib.chef_files('base')}/#{File.basename(bcrypt_script)} #{bcrypt_script} &&
      chown root.root #{bcrypt_script} && chmod 755 #{bcrypt_script}
    )
  end

  def self.uuid
    %x(apt-get install -y uuid) if %x(which uuid).strip.to_s.empty?
    %x(uuid -v 4).strip
  end

  def self.public_iface
    'eth0'
  end

  def self.public_port (chef, name, port, iface='world', protocol='tcp')
    return if is_docker # No iptables support on docker
    %x(apt-get install -u iptables) if %x(which iptables).strip.to_s.empty?
    case iface
      when 'world'
        interface = 'eth0'
      when 'local'
        interface = 'lo'
      else
        raise "invalid interface: #{iface}"
    end

    protocol_clause = "-p #{protocol}"
    inport_clause = port.nil? ? '' : "--dport #{port} --sport 513:65535"
    outport_clause = port.nil? ? '' : "--sport #{port} --dport 513:65535"

    input="-A INPUT -i #{interface} #{protocol_clause} #{inport_clause} -m state --state NEW,ESTABLISHED -j ACCEPT"
    output="-A OUTPUT #{protocol_clause} #{outport_clause} -m state --state ESTABLISHED -j ACCEPT"

    iptables_file="/etc/iptables.d/#{port}_#{name}"
    chef.template iptables_file do
      source 'iptables_entry.erb'
      owner 'root'
      group 'root'
      mode '0700'
      cookbook 'base'
      variables({ :input => input, :output => output })
    end

    refresh_firewall chef
  end

  def self.refresh_firewall (chef)
    return if is_docker # No iptables support on docker
    return if %x(which iptables).strip.to_s.empty?
    iptables_refresh='/etc/network/if-pre-up.d/iptables_load'
    chef.bash "reload iptables rules at #{Time.now} " do
      user 'root'
      code <<-EOF
if [ -x #{iptables_refresh} ] ; then
  #{iptables_refresh}
fi
      EOF
    end
  end

  def self.is_docker
    File.exist? '/etc/is-docker'
  end

  def self.enable_docker
    File.exist? '/etc/enable-docker'
  end

end
