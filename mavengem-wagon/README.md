# mavengem wagon

extend maven to use mavengem-protocol for configuring a rubygems
repository. this allows to use gem-artifacts as dependencies.

## usage

pom.xml setup

```
  ...
  <repositories>  
    <repository>
      <id>mavengems</id>
      <url>mavengem:http://rubygems.org</url>
    </repository>
  </repositories>
  
  <build>
    <extensions>
      <extension>
        <groupId>org.torquebox.mojo</groupId>
        <artifactId>mavengem-wagon</artifactId>
        <version>0.2.0</version>
      </extension>
    </extensions>
  </build>

</project>
```

the same with POM using ruby-DSL

```
repository :id => :mavengems, :url => 'mavengem:https://rubygems.org'

extension 'org.torquebox.mojo:mavengem-wagon:0.2.0'
```

the wagon extension allos the use of the **mavengem:** protocol in the
repository url.

## configuration

the configuration happens inside settings.xml (default location is
$HOME/.m2/settings.xml) and uses the **id** from the repository to
allow further configurations.

### cache directory for the mavengem protocol

```
<settings>
  <servers>
    <server>
      <id>mavengems</id>
      <configuration>
        <cachedir>${user.home}/.cachedir</cachedir>
      </configuration>
    </server>
  </servers>
</settings>
```

### username/password authentication

PENDING wating for a new release for the underlying nexus-ruby-tools
library to get this feature working

```
<settings>
  <servers>
    <server>
      <id>mavengems</id>
      <username>my_login</username>
      <password>my_password</password>
    </server>
  </servers>
</settings>
```

### mirror

use a mirror for the configured server

```
<settings>
  <servers>
    <server>
      <id>mavengems</id>
      <configuration>
        <mirror>https://rubygems.org</cachedir>
      </configuration>
    </server>
  </servers>
</settings>
```

the usename and password in a configuration with mirror will be used
for the mirror:

```
<settings>
  <servers>
    <server>
      <id>mavengems</id>
      <username>my_login</username>
      <password>my_password</password>
      <configuration>
        <mirror>https://rubygems.org</cachedir>
      </configuration>
    </server>
  </servers>
</settings>
```

## possible problems

warning like this might pop up but let the build pass.

```
[WARNING] Failure to transfer com.github.jnr:jffi:1.3.0-SNAPSHOT/maven-metadata.xml from mavengem:https://rubygems.org was cached in the local repository, resolution will not be reattempted until the update interval of mavengems has elapsed or updates are forced. Original error: Could not transfer metadata com.github.jnr:jffi:1.3.0-SNAPSHOT/maven-metadata.xml from/to mavengems (mavengem:https://rubygems.org): Cannot access mavengem:https://rubygems.org with type default using the available connector factories: BasicRepositoryConnectorFactory
[WARNING] Could not transfer metadata com.github.jnr:jnr-x86asm/maven-metadata.xml from/to mavengems (mavengem:https://rubygems.org): Cannot access mavengem:https://rubygems.org with type default using the available connector factories: BasicRepositoryConnectorFactory
```

the only way to avoid such warning or in case the build fails, is to use maven-3.3.x and add .mvn/extensions.xml to your project with:

```
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
  <extension>
    <groupId>org.torquebox.mojo</groupId>
    <artifactId>mavengem-wagon</artifactId>
    <version>0.2.0</version>
  </extension>
</extensions>
```

or
```
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
  <extension>
    <groupId>io.takari.polyglot</groupId>
    <artifactId>polyglot-ruby</artifactId>
    <version>0.1.16</version>
  </extension>
</extensions>
```

using any of the jruby-maven-plugins like
```
<plugin>
  <groupId>de.saumya.mojo</groupId>
  <artifactId>gem-maven-plugin</artifactId>
  <version>1.0.10</version>
  <extensions>true</extensions>
  ...
  <dependencies>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>compass</artifactId>
      <version>1.0.3</version>
      <type>gem</type>
    </dependency>
  </dependencies>
</plugin>
```

the extensions config set to ```true``` means that the gems get resolved before the ```mavengem``` gets registered, i.e. the ```mavengem``` protocol does not yet work. the .mvn/extensions.xml is the only way to fix this. maybe the extensions config of the gem-maven-plugin can be set to false. the resolution of the gems will work.