import java.io.*
import org.codehaus.plexus.util.FileUtils

String log = FileUtils.fileRead( new File( basedir, "build.log" ) );

[ "Downloaded from mavengems-with-mirror: mavengem:http://rubygems.org/rubygems/jar-dependencies/0.2.6/jar-dependencies-0.2.6.pom", "Downloaded from mavengems-with-mirror: mavengem:http://rubygems.org/rubygems/jar-dependencies/0.2.6/jar-dependencies-0.2.6.gem" ].each {
  
  if ( !log.contains( it ) ) throw new RuntimeException( "log file does not contain '" + it + "'" );

}

[ 'target/cachedir/https___rubygems_org/api/v2/rubygems/jar-dependencies/versions/0.2.6.json',
  'target/cachedir/https___rubygems_org/gems/j/jar-dependencies-0.2.6.gem',
  'target/cachedir/https___rubygems_org/info/jar-dependencies.compact' ].each {
  if ( !new File(basedir, it).exists() ) throw new RuntimeException( "expected file missing: '" + it + "'" );
}
true
