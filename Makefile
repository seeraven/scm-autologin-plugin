# -----------------------------------------------------------------------------
# SETTINGS
# -----------------------------------------------------------------------------

UBUNTU_VERSION     := 20.04
IMAGE_TAG          := scm-manager/plugin-buildenv:$(UBUNTU_VERSION)
CONTAINER_NAME     := scm_plugin_build
MAKEFILE_DIR       := $(shell dirname $(realpath $(firstword $(MAKEFILE_LIST))))
MAVEN_STORAGE      := $(abspath $(MAKEFILE_DIR)/maven_storage)
DIST_DIR           := $(abspath $(MAKEFILE_DIR)/dist)


# -----------------------------------------------------------------------------
# DEFAULT TARGETS
# -----------------------------------------------------------------------------
all: build docker_start scm_autologin_plugin_build docker_stop


# -----------------------------------------------------------------------------
# INFO TARGETS
# -----------------------------------------------------------------------------

.PHONY: info
info:
	@echo "Maven dir: $(MAVEN_STORAGE)"
	@echo "Dist dir:  $(DIST_DIR)"
	@echo "Username:  $(shell id -u -n)"
	@echo "User ID:   $(shell id -u)"
	@echo "Home:      ${HOME}"
	@echo "Groupname: $(shell id -g -n)"
	@echo "Group ID:  $(shell id -g)"


# -----------------------------------------------------------------------------
# DOCKER IMAGE BUILD TARGETS
# -----------------------------------------------------------------------------
.PHONY: build
build:
	@docker build --rm \
	              -f Dockerfile.build \
	              --build-arg UBUNTU_VERSION=$(UBUNTU_VERSION) \
	              -t $(IMAGE_TAG) \
	              .

.PHONY: build-nocache
build-nocache:
	@docker build --no-cache --pull --rm \
	              -f Dockerfile.build \
	              --build-arg UBUNTU_VERSION=$(UBUNTU_VERSION) \
	              -t $(IMAGE_TAG) \
	              .


# -----------------------------------------------------------------------------
# DOCKER CONTAINER TARGETS
# -----------------------------------------------------------------------------
.PHONY: docker_start
docker_start:
	@mkdir -p $(MAVEN_STORAGE) $(DIST_DIR)/scm-autologin-plugin $(DIST_DIR)/scmp
	@docker run --rm -d \
	  --name $(CONTAINER_NAME) \
	  -v $(MAVEN_STORAGE):/root/.m2 \
	  -v $(MAKEFILE_DIR):/work/scm-autologin-plugin \
	  -v $(DIST_DIR):/work/dist \
	  $(IMAGE_TAG) \
	  /bin/sleep infinity

.PHONY: docker_enter
docker_enter:
	@docker exec -ti $(CONTAINER_NAME) /bin/bash

.PHONY: docker_stop
docker_stop:
	@docker stop $(CONTAINER_NAME)


# -----------------------------------------------------------------------------
# PLUGIN BUILD TARGETS
# -----------------------------------------------------------------------------
.PHONY: scm_autologin_plugin_build
scm_autologin_plugin_build:
	@docker exec -ti $(CONTAINER_NAME) /bin/bash -c "mkdir -p scm-autologin-plugin-dist && cd scm-autologin-plugin && mvn scmp:install -DscmHome=../scm-autologin-plugin-dist && mvn scmp:package"
	@docker exec -ti $(CONTAINER_NAME) /bin/bash -c "rm -rf dist/scm-autologin-plugin/*; mv scm-autologin-plugin-dist/plugins/* dist/scm-autologin-plugin/"
	@docker exec -ti $(CONTAINER_NAME) /bin/bash -c "cp scm-autologin-plugin/target/*.scmp  dist/scmp"
	@docker exec -ti $(CONTAINER_NAME) /bin/bash -c "rm -rf scm-autologin-plugin/target"


# -----------------------------------------------------------------------------
# MAINTENANCE TARGETS
# -----------------------------------------------------------------------------
.PHONY: clean
clean:
	@find . -iname "*~" -exec rm -f {} \;
	@rm -rf dist
	@docker system prune -f


# -----------------------------------------------------------------------------
# EOF
# -----------------------------------------------------------------------------
