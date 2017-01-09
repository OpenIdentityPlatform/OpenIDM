# Sample DEVOPS-GETTINGSTARTED

This is the first "easy" sample. It leverages the existing 'Getting
Started' sample but with a [PostgreSQL](https://www.postgresql.org/)
repository.

Before you continue, install the Postgres and OpenIDM Docker images
described in the README-docker-k8. You won't be able to complete
this sample without them.

## Before you press the "Eject" button, Secrets & more...

The `openidm` deployment is using the Kubernetes `secrets` feature. But
we haven't currently described this in terms of manifests. So... before
you start you need to create the following secrets, based on the
`password.txt` file that will be created in the sample directory and the 2
"default" key stores :

    $ echo -n 'openidm' > openidm/samples/devops-gettingstarted/password.txt

    $ more openidm/samples/devops-gettingstarted/password.txt
    openidm

    $ kubectl create secret generic postgres-pass --from-file=openidm/samples/devops-gettingstarted/password.txt
    secret "postgres-pass" created

    $ kubectl get secrets
    NAME                  TYPE                                  DATA      AGE
    default-token-x4bb7   kubernetes.io/service-account-token   3         1h
    postgres-pass         Opaque                                1         23m




And finally the `secstores` secret :

    $ ls openidm/samples/devops-gettingstarted/
    README.md		keystore.jceks		script
    conf			openidm-deployment.yaml	truststore
    data			password.txt

    $ kubectl create secret generic secstores \
        --from-file=openidm/samples/devops-gettingstarted/keystore.jceks \
        --from-file=openidm/samples/devops-gettingstarted/truststore
    secret "secstores" created


    $ kubectl get secret secstores
    NAME        TYPE      DATA      AGE
    secstores   Opaque    2         22s

    $ kubectl describe secret secstores
    Name:		secstores
    Namespace:	default
    Labels:		<none>
    Annotations:	<none>

    Type:	Opaque

    Data
    ====
    keystore.jceks:	5686 bytes
    truststore:	122942 bytes

Once the `postgres-pass` and the `secstores` secrets are created, you
can proceed to the next steps.

The `postgres-pass` will be passed as an environment variable via
the Kubernetes container `env` directive ; while the key and trust
stores will be mounted in the pod via `volumeMounts`.

## Deploying the PostgreSQL repository

In order to start a pod with the repository for OpenIDM, we can simply
use the following command :

```
$ kubectl create -f openidm/samples/devops-postgres/postgres-deployment.yaml
service "postgres-openidm" created
persistentvolume "local-pv" created
persistentvolumeclaim "postgres-pv-claim" created
deployment "postgres-openidm" created
```

We can check if the pod was properly started :

```
$ kubectl get pods
NAME                                READY     STATUS    RESTARTS   AGE
postgres-openidm-3502169376-jg8y5   1/1       Running   0          23s
```

Now this is only telling us that the pod is running. We need to make
sure that it has initialized correctly with the OpenIDM scripts that
will create the appropriate tables, etc.

```
$ kubectl logs postgres-openidm-3502169376-inpf4

[...]
/docker-entrypoint.sh: running /docker-entrypoint-initdb.d/activiti.postgres.create.identity.sql
CREATE TABLE
CREATE TABLE
CREATE TABLE
CREATE TABLE
CREATE INDEX
ALTER TABLE
CREATE INDEX
ALTER TABLE


LOG:  received fast shutdown request
LOG:  aborting any active transactions
LOG:  autovacuum launcher shutting down
LOG:  shutting down
waiting for server to shut down....LOG:  database system is shut down
 done
server stopped

PostgreSQL init process complete; ready for start up.

LOG:  database system was shut down at 2016-10-21 01:14:51 UTC
LOG:  MultiXact member wraparound protections are now enabled
LOG:  database system is ready to accept connections
LOG:  autovacuum launcher started
```

## Starting OpenIDM

We can now let Kubernetes create a new pod for us that will run OpenIDM
and will connect to the Postgres repository we just created.

```
$ kubectl create -f openidm/samples/devops-gettingstarted/openidm-deployment.yaml
You have exposed your service on an external port on all nodes in your
cluster.  If you want to expose this service to the external internet, you may
need to set up firewall rules for the service port(s) (tcp:30088) to serve traffic.

See http://releases.k8s.io/release-1.3/docs/user-guide/services-firewalls.md for more details.
service "openidm" created
deployment "openidm" created
```

Check to make sure the new pod was created and that OpenIDM is running :

```$ kubectl get pods
NAME                                READY     STATUS    RESTARTS   AGE
openidm-2594471299-1ne90            1/1       Running   0          4s
postgres-openidm-3502169376-inpf4   1/1       Running   0          1m

$ kubectl logs openidm-2594471299-1ne90
Using OPENIDM_HOME:   /opt/idmuser/openidm
Using PROJECT_HOME:   /opt/idmuser/openidm/samples/devops-gettingstarted
Using OPENIDM_OPTS:    -Dopenidm.keystore.password=changeit -Dopenidm.truststore.password=changeit -Dopenidm.node.id=028f063a-972c-11e6-b752-0242ac110004 -Dopenidm.repo.host=postgres-openidm -Dopenidm.repo.port=5432 -Dopenidm.repo.user=openidm -Dopenidm.repo.password=openidm

Using LOGGING_CONFIG: -Djava.util.logging.config.file=/opt/idmuser/openidm/samples/devops-gettingstarted/conf/logging.properties
Using NODE_ID : 028f063a-972c-11e6-b752-0242ac110004
Using boot properties at /opt/idmuser/openidm/samples/devops-gettingstarted/conf/boot/boot.properties
ShellTUI: No standard input...exiting.
-> OpenIDM version "5.5.0-SNAPSHOT" (revision: acd3777) jenkins-OpenIDM - postcommit-1918 origin/master
```

Overall you should have something like this :

```
$ kubectl get services,deployments,pvc,pv,pods
NAME                                CLUSTER-IP   EXTERNAL-IP   PORT(S)      AGE
kubernetes                          10.0.0.1     <none>        443/TCP      22h
openidm                             10.0.0.249   <nodes>       80/TCP       38m
postgres-openidm                    None         <none>        5432/TCP     40m
NAME                                DESIRED      CURRENT       UP-TO-DATE   AVAILABLE                   AGE
openidm                             1            1             1            1                           38m
postgres-openidm                    1            1             1            1                           40m
NAME                                STATUS       VOLUME        CAPACITY     ACCESSMODES                 AGE
postgres-pv-claim                   Bound        local-pv      5Gi          RWO                         40m
NAME                                CAPACITY     ACCESSMODES   STATUS       CLAIM                       REASON    AGE
local-pv                            5Gi          RWO           Bound        default/postgres-pv-claim             40m
NAME                                READY        STATUS        RESTARTS     AGE
openidm-2594471299-1ne90            1/1          Running       0            38m
postgres-openidm-3502169376-inpf4   1/1          Running       0            40m
```

## Accessing OpenIDM

You should now be able to access OpenIDM on port 30088 of your Kubernetes
cluster IP address.

The best way to do this is to use the following command :

    $ minikube dashboard

The Kubernetes Dashboard will start in your web browser. By simply
changing the port number you should be able to access OpenIDM.

The port number is defined in the `openidm-deployment.yaml` file :

```
  ports:
    - port: 80
      targetPort: 8080
      nodePort: 30088
```

You can also check the IP address that your cluster is running on in
Minikube via :

```
$ minikube ip
192.168.64.12
```

So, for my instance, OpenIDM was available at : http://192.168.64.12:30088/

## Scaling it up and down

Now that we have 1 instance of OpenIDM running, we can use the built-in
feature of Kubernetes to scale the number of pods :

```$ kubectl scale --replicas=3 deployment/openidm
deployment "openidm" scaled

$ kubectl get pods -l "tier=frontend"
NAME                       READY     STATUS    RESTARTS   AGE
openidm-1259037605-57pim   1/1       Running   0          28m
openidm-1259037605-jour3   1/1       Running   0          7m
openidm-1259037605-o2zii   1/1       Running   0          7m
```

If you access the Admin console and use the "Cluster Node Status"
widget, you will see all 3 nodes that were registered with the cluster
service.

To scale down, simply specify the number of replicas you want :

```$ kubectl scale --replicas=1 deployment/openidm
deployment "openidm" scaled

$ kubectl get pods -l "tier=frontend"
NAME                       READY     STATUS    RESTARTS   AGE
openidm-1259037605-57pim   1/1       Running   0          31m
```


## Taking it down...

To get rid of each deployment simply delete via kubectl using the
same Kubernetes artifacts :

```
$ kubectl delete -f openidm/samples/devops-gettingstarted/openidm-deployment.yaml
service "openidm" deleted
deployment "openidm" deleted

$ kubectl delete -f openidm/samples/devops-postgres/postgres-deployment.yaml
service "postgres-openidm" deleted
persistentvolume "local-pv" deleted
persistentvolumeclaim "postgres-pv-claim" deleted
deployment "postgres-openidm" deleted
```

Note : as discussed below, taking down the Postgres deployment in one
fell swoop might lead to the Persistent Volume not being recycled
properly.

# Troubleshooting

Here are some tips in case you run into trouble...

## PostgreSQL issues

If you see the following output only (and nothing else) when running
the logs command :

```
$ kubectl logs postgres-openidm-3502169376-jg8y5
LOG:  database system was shut down at 2016-10-21 00:43:21 UTC
LOG:  MultiXact member wraparound protections are now enabled
LOG:  database system is ready to accept connections
LOG:  autovacuum launcher started
```

... then you've probably had a previous instance of that deployment
running and the Persistent Volume wasn't formatted when it was deleted
(this is a problem when running a `kubectl delete -f` on the postgres
deployment file).

So here's what you'll need to do (otherwise OpenIDM will start with
some encryption errors) :

```
$ kubectl delete deployment postgres-openidm
deployment "postgres-openidm" deleted
$ kubectl delete service postgres-openidm
service "postgres-openidm" deleted
$ kubectl delete pvc postgres-pv-claim
persistentvolumeclaim "postgres-pv-claim" deleted
$ kubectl delete pv local-pv
persistentvolume "local-pv" deleted
```

And then check that nothing is left dangling...

```
$ kubectl get services,deployments,pvc,pv,pods
NAME         CLUSTER-IP   EXTERNAL-IP   PORT(S)   AGE
kubernetes   10.0.0.1     <none>        443/TCP   21h
```
