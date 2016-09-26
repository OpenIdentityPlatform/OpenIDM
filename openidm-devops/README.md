# Module OPENIDM-DEVOPS

This module is a compilation of Dockerfile, YAML files for Kubernetes
configuration and a pom.xml that makes it easy for a developer to
utilize the `mvn` command to :

* build a docker image from a provided Dockerfile
* generate a YAML resource file from provided K8s files
* deploy the resulting configuration in a local K8s instance

At this time, the requirements (and limitations) are that the following
components are running on the local machine :

1. docker
1. minikube (tested with xhyve)


## Required Components

### Installing Docker

Docker is pretty easy to install. Please follow the [instructions](https://www.docker.com/products/docker#/mac)
to install the latest verison of Docker for Mac (using xhyve).

If you already have [Docker Toolbox](https://docs.docker.com/toolbox/overview/), 
that's fine, but the instructions below might differ (i.e. you're on your
own).

For other platforms, please check the [Docker documentation](https://docs.docker.com/engine/installation/).

Once Docker is installed, you can test it out with the following command
and get a result back -- if Docker isn't installed you will get an error
from your shell :

```bash
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
 ```

### Installing Kubernetes

Kubernetes is still evolving a lot. While the simplest way to use
Kubernetes is certainly to use Google's [container enginer](https://cloud.google.com/container-engine/),
i.e. GKE and I really encourage you to check it out, the most intuitive
for developers is probably the local solution presented by [Minikube](https://github.com/kubernetes/minikube/).

So the instructions below pertain only to Minikube deployments.

Minikube can run with different virtualization tools (virtualbox, kvm, etc.).
But [xhyve](https://github.com/kubernetes/minikube/blob/master/DRIVERS.md#xhyve-driver)
was used since it's fairly lightweight compared to the other options.

So, first off [install](https://github.com/kubernetes/minikube/releases)
Minikube and confirm that it is functioninig properly by following the
steps in the [Quickstart](https://github.com/kubernetes/minikube/#quickstart)

You should be able to get the minikube version with no errors :

```bash
$ minikube version
minikube version: v0.9.0
```

## Putting things together

### Starting everything

A standard configuration would assume that Docker is started when the
Mac is started, but if that's not the case (check your Docker whale
icon in your Mac's status bar), then start Docker first.

Minikube needs to be started as well ; please make sure you followed the
steps on how to configure it the first time when using xhyve since
virtualbox is the default virtualization engine.

```
$ minikube start --vm-driver=xhyve [...other config options]
```

Otherwise, you will simply issue the following command and you should
obtain similar results as indicated below :

```
$ minikube start
Starting local Kubernetes cluster...
Kubectl is now configured to use the cluster.
```

Now in order for Docker to "execute" within the same space as Minikube,
you will need to run :

```
$ eval $(minikube docker-env)
```

Since Minikube runs several containers as part of the Kubernetes framework,
you should see something similar to the following now :

```
$ docker ps
CONTAINER ID        IMAGE                                                        COMMAND                  CREATED             STATUS              PORTS               NAMES
7c7f3f3ffc6c        gcr.io/google_containers/kubernetes-dashboard-amd64:v1.1.0   "/dashboard --port=90"   27 minutes ago      Up 27 minutes                           k8s_kubernetes-dashboard.b3dad817_kubernetes-dashboard-7izep_kube-system_0c722cd1-5a5c-11e6-b60d-4ea7f0f3ed1a_4e7f99b9
fd07bdf1c017        gcr.io/google_containers/pause-amd64:3.0                     "/pause"                 27 minutes ago      Up 27 minutes                           k8s_POD.d5030529_openidm-devops-3650949797-0lgwk_verycloudy_9b1fae4a-81c8-11e6-bbab-520f42146fc7_ad685ccf
c1fce80f8b22        gcr.io/google_containers/pause-amd64:3.0                     "/pause"                 27 minutes ago      Up 27 minutes                           k8s_POD.2225036b_kubernetes-dashboard-7izep_kube-system_0c722cd1-5a5c-11e6-b60d-4ea7f0f3ed1a_34df2ef4
d581e99a4488        gcr.io/google-containers/kube-addon-manager-amd64:v2         "/opt/kube-addons.sh"    27 minutes ago      Up 27 minutes                           k8s_kube-addon-manager.a1c58ca2_kube-addon-manager-boot2docker_kube-system_48abed82af93bb0b941173334110923f_01dd1554
c981440fa134        gcr.io/google_containers/pause-amd64:3.0                     "/pause"                 27 minutes ago      Up 27 minutes                           k8s_POD.d8dbe16c_kube-addon-manager-boot2docker_kube-system_48abed82af93bb0b941173334110923f_1fe99adb
```

You can also get access to the Kubernetes Dashboard via :

```
$ minikube dashboard
Opening kubernetes dashboard in default browser...
```

### Using kubectl

`kubectl` is your go-to (pun intended) command tool to control the
different Kubernetes abstractions : pods, deployments, services, contexts,
namespaces, volumes, etc.

You can get a good sense of what's going on without using the Dashboard :

```
$ kubectl get deployments,svc
NAME         CLUSTER-IP   EXTERNAL-IP   PORT(S)   AGE
kubernetes   10.0.0.1     <none>        443/TCP   50d
```

For more info, please the [section](https://github.com/kubernetes/minikube/#interacting-with-your-cluster)
on kubectl and also the [reference](http://kubernetes.io/docs/user-guide/kubectl-overview/) .

## Running Kubernetes from mvn

### Building the docker images
We are now going to use the [Fabric8 Maven Plugin](https://github.com/fabric8io/fabric8-maven-plugin)
(fmp) to build the necessary docker images to run OpenIDM with the
"Getting Started" sample on Postgres.

```
$ cd Development/Stash/openidm
$ git pull
$ mvn clean install
```

This should build the latest version of OpenIDM (currently 5.0.0-SNAPSHOT). 
Now let's play in our dev-ops sandbox :

```
$ cd openidm-devops
$ mvn fabric8:build  -Dk8env=getting-started
[...]
```

If your build is successful, now you should see the following docker
images :

```
$ docker images forgerock/*
REPOSITORY                   TAG                 IMAGE ID            CREATED             SIZE
forgerock/openidm-postgres   latest              ee371984988a        9 minutes ago       459.3 MB
forgerock/openidm-zip        latest              0337bbcc64bb        10 minutes ago      943.3 MB
```

### Creating the Kubernetes resources

This is technically an optional step, and we should just be able to
call the `deploy` goal of the maven plugin ; but we're currently running
into a [problem](https://github.com/fabric8io/fabric8/issues/6389) with 
fmp.

So we have to first generate the resources we need to deploy our
environment.

```
$ mvn fabric8:resource  -Dk8env=getting-started
... 
$ ls target/classes/META-INF/fabric8/
kubernetes	kubernetes.json	kubernetes.yml	openshift	openshift.json	openshift.yml
```

As you can see several files were created in the `target` directory. The
one of interest for us in the `kubernetes.yml` file. Which, because
of the aformentioned issue we will need to edit :

```
$ vi target/classes/META-INF/fabric8/kubernetes.yml

```

There are 2 sections that we need to remove that look like this :

```
        - env:
          - name: "KUBERNETES_NAMESPACE"
            valueFrom:
              fieldRef:
                fieldPath: "metadata.namespace"
          image: "forgerock/openidm-postgres"
          imagePullPolicy: "IfNotPresent"
          name: "forgerock-openidm-devops"
          securityContext:
            privileged: false
```

Here's the diff between the original file and what will work :

```
$ diff target/classes/META-INF/fabric8/kubernetes.yml ./kubernetes.yml
153,162d152
<         - env:
<           - name: "KUBERNETES_NAMESPACE"
<             valueFrom:
<               fieldRef:
<                 fieldPath: "metadata.namespace"
<           image: "forgerock/openidm-postgres"
<           imagePullPolicy: "IfNotPresent"
<           name: "forgerock-openidm-devops"
<           securityContext:
<             privileged: false
227,236d216
<         - env:
<           - name: "KUBERNETES_NAMESPACE"
<             valueFrom:
<               fieldRef:
<                 fieldPath: "metadata.namespace"
<           image: "forgerock/openidm-postgres"
<           imagePullPolicy: "IfNotPresent"
<           name: "forgerock-openidm-devops"
<           securityContext:
<             privileged: false
```

### Starting the deployment

First let's make sure that only the kubernetes service is running :

```
$ kubectl get svc,deployments,pv,pvc,pods
NAME         CLUSTER-IP   EXTERNAL-IP   PORT(S)   AGE
kubernetes   10.0.0.1     <none>        443/TCP   22h
```

Now it's okay if you have other services or deployments running, as
long at they don't conflict with what is generated in the `kubernetes.yml`
file.

Note : make sure you've modified the `target/classes/META-INF/fabric8/kubernetes.yml`
file as indicated above. This is what we'll use to create the deployment.

```
$ kubectl create -f target/classes/META-INF/fabric8/kubernetes.yml
You have exposed your service on an external port on all nodes in your
cluster.  If you want to expose this service to the external internet, you may
need to set up firewall rules for the service port(s) (tcp:30080) to serve traffic.

See http://releases.k8s.io/release-1.3/docs/user-guide/services-firewalls.md for more details.
service "openidm" created
service "postgres" created
persistentvolume "local-pv" created
persistentvolumeclaim "postgres-pv-claim" created
deployment "openidm" created
deployment "postgres" created

$ kubectl get svc,deployments,pv,pvc,pods
NAME                        CLUSTER-IP   EXTERNAL-IP   PORT(S)      AGE
kubernetes                  10.0.0.1     <none>        443/TCP      22h
openidm                     10.0.0.232   <nodes>       8080/TCP     41s
postgres                    None         <none>        5432/TCP     41s
NAME                        DESIRED      CURRENT       UP-TO-DATE   AVAILABLE                   AGE
openidm                     1            1             1            1                           41s
postgres                    1            1             1            1                           41s
NAME                        CAPACITY     ACCESSMODES   STATUS       CLAIM                       REASON    AGE
local-pv                    5Gi          RWO           Bound        default/postgres-pv-claim             41s
NAME                        STATUS       VOLUME        CAPACITY     ACCESSMODES                 AGE
postgres-pv-claim           Bound        local-pv      5Gi          RWO                         41s
NAME                        READY        STATUS        RESTARTS     AGE
openidm-2347877805-51vnj    1/1          Running       0            41s
postgres-4084076017-r0xo8   1/1          Running       0            41s
```

You should now be able to access OpenIDM on port 30080 of your Kubernetes
cluster IP address.

The best way to do this is to use the following command :

```
$ minikube dashboard
```

The Kubernetes Dashboard will start in your web browser. By simply
changing the port number you should be able to access OpenIDM.

For my instance this was : http://192.168.64.4:30080/

Note : once the issue is resolved, I will update this section with
instructions on how to use the [`fabric8:deploy`](https://maven.fabric8.io/#fabric8:deploy) goal.

## Taking it down

The best way to completely clean what you just did, is to use the same
YAML file :

```
$ kubectl delete -f target/classes/META-INF/fabric8/kubernetes.yml
service "openidm" deleted
service "postgres" deleted
persistentvolume "local-pv" deleted
persistentvolumeclaim "postgres-pv-claim" deleted
deployment "openidm" deleted
deployment "postgres" deleted

$ kubectl get svc,deployments,pv,pvc,pods
NAME         CLUSTER-IP   EXTERNAL-IP   PORT(S)   AGE
kubernetes   10.0.0.1     <none>        443/TCP   23h
```

The last command shows that everything has been deleted properly.

Note : this section will be udpated with the [`fabric8:undeploy`](https://maven.fabric8.io/#fabric8:undeploy)
goal.

## Future improvements

We're really only scratching the surface of what we can do. Some ideas
for future enhancements would be :

* use property substitution of the `k8env` property to build any sample
from the cmd line. Right now we're just using this property to trigger
the build profile, but we could do much more with it
* use the `deploy` and `undeploy` goals from the start (deploy will
generate the necessary images and resources)
* add new components (like OpenDJ) to enable other samples
* add new repositories and trigger them with build properties
* use fabric8 fully to watch new commits and create the environments
automatically (this is a little heavier for a development machine)

So much more to do... 