[![Build & Test](https://github.com/Sebastian-0/SmallCloudService/actions/workflows/main.yml/badge.svg)](https://github.com/Sebastian-0/SmallCloudService/actions/workflows/main.yml)

# Small Cloud Service
This is a small full stack application for handling a database of synonyms. The application is deployed to AWS and is accessible here: http://synonymlibrary.info

## Structure
The app is built with a backend in Java with Jetty and Jersey, and with a frontend built with Svelte. For hosting it's using AWS Elastic Beanstalk, Cloudfront and S3.

### Backend
The backend is implemented as a Jetty HTTP server with a simple API. There is one endpoint for adding synonyms and one for fetching a paginated list. The data is not persisted so it's lost upon restart. For this reason (and to make hosting easier) the server is implemented as a single node, which has some obvious disadvantages related to uptime, load distribution and maintenance. However, there is also a functioning multi-node implementation in the branch `multi-node-support`.

The multi-node implementation requires that you define a cluster (a collection of hosts) which it then will keep in sync. If you need to do maintenance or upgrade a node you can simply remove it from the cluster be doing a new cluster definition where the node is missing. Then when you are done you can include it again, whereupon it will get all changes imported.

Using this you can do rolling restarts/upgrades on the entire cluster without downtime, and you can also easily expand with more nodes if the load is high.

The code for the backend is located in the subfolder `Server`.

### Frontend
The frontend is implemented using the [Svelte framework](https://svelte.dev), which is a fast and modern library for building reactive applications.

The code for the frontend is located in the subfolder `App`

### Deployment
The application is hosted in AWS in two separate parts, the web app is hosted in S3 using Cloudfront and the backend server is hosted by Elastic Beanstalk. Deploying new versions can be done by invoking GitHub Actions directly from the repository.

Elastic Beanstalk is not ideal for hosting applications where the server nodes need to communicate with eachother, so to use the `multi-node-support` branch effectively the hosting would need to be replaced with direct hosting of a few nodes in EC2, combined with a load balancer and some scripts to properly handle upgrades, etc...

## Building
Bulding locally is simple.

### The server
To build the server you can run `./gradlew build` which will produce the artifact `Server/build/Server-1.0-SNAPSHOT.zip`

### The web app
To build the web app you need to enter the `App` directory and run `npm install` followed by `npm run build`, or `npm run dev` if you want to start the server locally.

## Future improvements
There are a whole lot of weaknesses with the current implementation, some future improvements are:
- Persisting data to the disk.
- Multi-node implementation and deployment.
- Improved security: the backend is available from the web and could easily be attacked. Some kind of authentication or private network could make it a lot better.
- Support for removing synonyms.

## License
This code is freely available as long as you comply with the GPLv3 license, see `LICENSE` for details.