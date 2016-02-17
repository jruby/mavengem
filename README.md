# mavengem

this project consists of two modules. one is converting a rubygems repository
into maven repository. this happens either via registered the protocol
handler in JVM or by using a special ```MavenGemURLConnection``` which
turns a rubygems repository on the fly to maven repository.

the second module uses this to add a transport extension to maven
allowing to use gems from a rubygems repository as gem artifacts
inside a POM. see also [http://maven.apache.org/wagon/](maven-wagon).

License
-------

As the wagon code was more or less stolen from EPL licensed code
[https://github.com/apache/maven-wagon/blob/master/wagon-providers/wagon-http-lightweight/src/main/java/org/apache/maven/wagon/providers/http/LightweightHttpWagon.java](LightweightHttpWagon.java)
this code is under the same license: EPL

Contributing
------------

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

meta-fu
-------

enjoy :) 
