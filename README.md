# admiral
Controls your fleet of docker containers.

## Configuration Variable Files (not Environment Variable Files)
Admiral supports environment variable files like docker-compose, but Admiral also supports something called
"configuration variable files" that docker-compose does not support.

Let's begin by showing what docker-compose does.  You can control the configuration of docker-compose.yaml
through the specially-named ".env" file like this:

```yaml
#file=.env
IMAGE_VERSION:1.34.7
```
```yaml
#file=docker-compose.yaml
services:
  sample1:
    image: "myappimage:${IMAGE_VERSION}"
```
```yaml
#docker-compose config
services:
  sample1:
    image: myappimage:1.34.7
version: '3.9'
```

Now what we wish docker-compose would do... let us put IMAGE_VERSION in a file
other than .env, like maybe production.env.  This is what happens when you do:

```yaml
#file=production.env
IMAGE_VERSION:1.34.7
```
```yaml
#file=docker-compose.yaml
services:
  sample1:
    env_file: production.env
    image: "myappimage:${IMAGE_VERSION}"
```
```yaml
#docker-compose config
WARNING: The IMAGE_VERSION variable is not set. Defaulting to a blank string.
services:
  sample1:
    environment:
      IMAGE_VERSION: 1.34.7
    image: 'myappimage:'
version: '3.9'
```

You see: docker-compose takes the contents of env_file and does two things:
1. It sends the variable to the containerController
2. It **doesn't** make that variable available to the docker-compose configuration.

What we want is a named .env file (a **config** file, not an **environment** file) that does the opposite:
1. It **doesn't** send the variable to the containerController
2. It makes that variable available to the docker-compose configuration.

There are plenty of people begging for this:
* https://github.com/docker/compose/issues/6170
* https://github.com/docker/compose/issues/8379

Admiral delivers!  And to differentiate config files from environment files, we can use an
extension of ".config" or the brief ".conf" instead of ".env".

(I'd rather talk about "the Production Config file" than "the Production Conf file" so just use .config
and deal with typing the extra two characters.)

```yaml
x-admiral-project:
  config_file: production.config
```

There is another docker-compose "won'tfix" that Admiral provides:
* https://github.com/docker/compose/issues/745

```yaml
x-admiral-project:
  name:
```
