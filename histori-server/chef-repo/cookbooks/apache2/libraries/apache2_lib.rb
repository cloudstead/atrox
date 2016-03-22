require 'securerandom'

class Chef::Recipe::Apache2

  def self.reload(chef, reason = nil)
    reason = reason.to_s.empty? ? '' : "(#{reason})"
    chef.bash "restart Apache #{reason}" do
      user 'root'
      code <<-EOH
if [[ "$(service apache2 status)" =~ "not running" ]] ; then
  service apache2 start
else
  service apache2 reload
fi
EOH
    end
  end

  def self.get_server_name (preset, mode, hostname, app_name)
    return preset unless preset.to_s.empty?
    (mode == :proxy_root || mode == :vhost_root) ? hostname : "#{app_name}-#{hostname}"
  end

  def self.new_module (chef, module_name)
    chef.cookbook_file "/usr/lib/apache2/modules/mod_#{module_name}.so" do
      owner 'root'
      group 'root'
      mode '0644'
      action :create
    end
    chef.template "/etc/apache2/mods-enabled/#{module_name}.load" do
      source 'module.load.erb'
      cookbook 'apache'
      owner 'root'
      group 'root'
      mode '0644'
      variables ({ :module_name => module_name })
      action :create
    end
  end

  def self.enable_module (chef, module_name)
    chef.bash "enable Apache module: #{module_name}" do
      user 'root'
      cwd '/tmp'
      code <<-EOH

if [[ #{module_name} =~ mpm_.+ ]] ; then
  # disable any existing mpm modules (there should be only 1, but just in case...)
  for mpm in $(find /etc/apache2/mods-enabled -name "mpm_*.load" | xargs basename | awk -F '.' '{print $1}') ; do
    a2dismod ${mpm}
  done

elif [ #{module_name} == "php5-fpm" ] ; then
  # disable existing php module if enabled
  if [ -e /etc/apache2/mods-enabled/php5.load ] ; then
    a2dismod php5
  fi
fi

a2enmod #{module_name}
      EOH
      not_if { File.exist? "/etc/apache2/mods-enabled/#{module_name}.load" }
    end
  end

  def self.disable_module (chef, module_name)
    chef.bash "disable Apache module: #{module_name}" do
      user 'root'
      cwd '/tmp'
      code <<-EOH
a2dismod #{module_name}
      EOH
      only_if { File.exist? "/etc/apache2/mods-enabled/#{module_name}.load" }
    end
  end

  def self.normalize_mount(mount)
    # ensure it begins with a slash and does not end with a slash (unless it is the single-char '/')
    mount ||= '/'
    mount = "/#{mount}" unless mount.start_with? '/'
    (mount.end_with? '/' && mount != '/') ? mount[0 .. -2] : mount
  end

  def self.dir_base (dir)
    dir.sub('@doc_root', 'doc_root').gsub('/', '_')
  end

  def self.dir_config_path (app_name, base)
    "/etc/apache2/apps/#{app_name}/dir_#{base}.conf"
  end

  def self.loc_base (loc)
    if loc.to_s == '' || loc == '/'
      return 'location_root'
    end
    if loc.start_with? '/'
      return "location_root_#{loc[1..loc.length].gsub('/', '_')}"
    else
      return "location_#{loc.gsub('/', '_')}"
    end
  end

  def self.loc_config_path (app_name, base)
    "/etc/apache2/apps/#{app_name}/#{base}.conf"
  end

  def self.enable_site(chef, site_name)
    chef.bash "enable Apache site: #{site_name}" do
      user 'root'
      cwd '/tmp'
      code <<-EOH
a2ensite #{site_name}
      EOH
    end
  end

  def self.disable_site (chef, site_name)
    site_name = '000-default' if site_name == 'default'
    chef.bash "disable Apache site: #{site_name}" do
      user 'root'
      cwd '/tmp'
      code <<-EOH
# always return true because site may already be disabled
a2dissite #{site_name} || true
      EOH
    end
  end

  def self.add_digest_user(digest_passwd_file, realm, user, password)
    unless File.exist?(digest_passwd_file)
      File.open(digest_passwd_file, 'w') {}
    end
    HTAuth::DigestFile.open(digest_passwd_file) do |df|
      df.add_or_update(user, realm, password)
    end
  end

end
