# Sample DEVOPS-OPENIDM

This sample is a foundation for the rest of the DevOps samples. Its
sole purpose is to provide the Dockerfile and a shell script that will
be used build the OpenIDM docker image.

Below is a collection of information on how to install Docker,
Kubernetes and how to make sure those required components are running
properly before jumping into the DevOps samples.

This README will help with :

* building a docker image from the provided Dockerfile
* deploying the provided configuration in a local K8s instance

At this time, the requirements (and limitations) are that the following
components are running on the local machine :

1. docker
1. minikube (tested with xhyve)


## Required Components

### Installing Docker

Docker is pretty easy to install. Please follow the [instructions](https://www.docker.com/products/docker#/mac)
to install the latest version of Docker for Mac (using xhyve).

If you already have [Docker Toolbox](https://docs.docker.com/toolbox/overview/), 
that's fine, but the instructions below might differ (i.e. you're on your
own).

For other platforms, please check the [Docker documentation](https://docs.docker.com/engine/installation/).

Once Docker is installed, you can test it out with the following command
and get a result back -- if Docker isn't installed you will get an error
from your shell :

    $ docker version
    Client:
     Version:      1.12.1
     API version:  1.23
     Go version:   go1.7.1
     Git commit:   6f9534c
     Built:        Thu Sep  8 10:31:18 2016
     OS/Arch:      darwin/amd64
    
    Server:
     Version:      1.11.1
     API version:  1.23
     Go version:   go1.5.4
     Git commit:   5604cbe
     Built:        Wed Apr 27 00:34:20 2016
     OS/Arch:      linux/amd64

### Installing Kubernetes

Kubernetes is still evolving a lot. While the simplest way to use
Kubernetes is certainly to use Google's [container engine](https://cloud.google.com/container-engine/),
i.e. GKE and I really encourage you to check it out, the most intuitive
for developers is probably the local solution presented by [Minikube](https://github.com/kubernetes/minikube/).

So the instructions below pertain only to **Minikube deployments**.

Minikube can run with different virtualization tools (virtualbox, kvm, etc.).
But [xhyve](https://github.com/kubernetes/minikube/blob/master/DRIVERS.md#xhyve-driver)
was used since it's fairly lightweight compared to the other options.

So, first off [install](https://github.com/kubernetes/minikube/releases)
Minikube and confirm that it is functioning properly by following the
steps in the [Quickstart](https://github.com/kubernetes/minikube/#quickstart)

You should be able to get the minikube version with no errors :

    $ minikube version
    minikube version: v0.9.0


## Putting things together

### Starting everything

A standard configuration would assume that Docker is started when the
Mac is started, but if that's not the case (check your Docker whale
icon in your Mac's status bar), then start Docker first.

Minikube needs to be started as well ; please make sure you followed the
steps on how to configure it the first time when using xhyve since
virtualbox is the default virtualization engine (or use virtualbox if
you're comfortable with that option).

    $ minikube start --vm-driver=xhyve [...other config options]

Otherwise, you will simply issue the following command and you should
obtain similar results as indicated below :

    $ minikube start
    Starting local Kubernetes cluster...
    Kubectl is now configured to use the cluster.

Now in order for Docker to "execute" within the same space as Minikube,
you will need to run :

    $ eval $(minikube docker-env)

Since Minikube runs several containers as part of the Kubernetes framework,
you should see something similar to the following now :

    $ docker ps
    CONTAINER ID        IMAGE                                                        COMMAND                  CREATED             STATUS              PORTS               NAMES
    7c7f3f3ffc6c        gcr.io/google_containers/kubernetes-dashboard-amd64:v1.1.0   "/dashboard --port=90"   27 minutes ago      Up 27 minutes                           k8s_kubernetes-dashboard.b3dad817_kubernetes-dashboard-7izep_kube-system_0c722cd1-5a5c-11e6-b60d-4ea7f0f3ed1a_4e7f99b9
    fd07bdf1c017        gcr.io/google_containers/pause-amd64:3.0                     "/pause"                 27 minutes ago      Up 27 minutes                           k8s_POD.d5030529_openidm-devops-3650949797-0lgwk_verycloudy_9b1fae4a-81c8-11e6-bbab-520f42146fc7_ad685ccf
    c1fce80f8b22        gcr.io/google_containers/pause-amd64:3.0                     "/pause"                 27 minutes ago      Up 27 minutes                           k8s_POD.2225036b_kubernetes-dashboard-7izep_kube-system_0c722cd1-5a5c-11e6-b60d-4ea7f0f3ed1a_34df2ef4
    d581e99a4488        gcr.io/google-containers/kube-addon-manager-amd64:v2         "/opt/kube-addons.sh"    27 minutes ago      Up 27 minutes                           k8s_kube-addon-manager.a1c58ca2_kube-addon-manager-boot2docker_kube-system_48abed82af93bb0b941173334110923f_01dd1554
    c981440fa134        gcr.io/google_containers/pause-amd64:3.0                     "/pause"                 27 minutes ago      Up 27 minutes                           k8s_POD.d8dbe16c_kube-addon-manager-boot2docker_kube-system_48abed82af93bb0b941173334110923f_1fe99adb

You can also get access to the Kubernetes Dashboard via :

    $ minikube dashboard
    Opening kubernetes dashboard in default browser...

### Using kubectl

`kubectl` is your go-to (pun intended) command tool to control the
different Kubernetes abstractions : pods, deployments, services, contexts,
namespaces, volumes, etc.

You can get a good sense of what's going on without using the Dashboard :

    $ kubectl get deployments,svc
    NAME         CLUSTER-IP   EXTERNAL-IP   PORT(S)   AGE
    kubernetes   10.0.0.1     <none>        443/TCP   50d

For more info, please the [section](https://github.com/kubernetes/minikube/#interacting-with-your-cluster)
on kubectl and also the [reference](http://kubernetes.io/docs/user-guide/kubectl-overview/) .

## Before you start using the DevOps samples

### The docker images
The Kubernetes manifests provided in the different DevOps samples all
rely on the presence of a few Docker images in the local Docker registry.

At a minimum the following images have to be built :

* forgerock/openidm
* forgerock/openidm-postgres

Note that the appropriate tags will be needed to match the Kubernetes
manifests.

#### Building the OpenIDM image

The Dockerfile for the OpenIDM image is located at the base of the
OpenIDM directory where you unzipped the OpenIDM zip file. The main
reason for this is that, during the build process, Docker passes the
current working directory as the place where resources for the new image
will be found. What better place then than where the content of OpenIDM
is located ?

So, in order to build the OpenIDM docker image :

```
$ unzip openidm-${project.version}
$ cd openidm
$ docker build -t forgerock/openidm:${project.version} .
[...]
```

Once the build is successful you can check the presence of the OpenIDM
image in your local repository :

```
$ docker images forgerock/*
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
forgerock/openidm   5.0.0-SNAPSHOT      467a8d6fc551        25 seconds ago      864.9 MB
```

#### Building the Postgres image

We are now going to build the postgres image to ensure that the docker
image for our repository is available. From the directory where you
unzipped the OpenIDM zip file, execute the following :

    $ cd openidm/samples/devops-postgres/
    $ ls
    Dockerfile			postgres-deployment.yaml	scripts
    $ docker build -t forgerock/openidm-postgres:${project.version} .

This should build the postgres image from the latest version of OpenIDM
that you obtained -- check for the successful message.

You can then see if your image is properly stored in your local image
repository :

```
$ docker images forgerock/*
REPOSITORY                   TAG                 IMAGE ID            CREATED             SIZE
forgerock/openidm-postgres   5.0.0-SNAPSHOT      fdda0147c6ce        10 seconds ago      265.7 MB
forgerock/openidm            5.0.0-SNAPSHOT      467a8d6fc551        3 minutes ago       864.9 MB
```

## Final thoughts

You should now be able to run the DevOps samples simply by following
the instructions for each of them. If more images are needed, the
instructions to build those should be provided.