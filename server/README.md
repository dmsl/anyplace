# Anyplace Server

### Project Structure  
```
.
├── anyplace_tiler
├── app
│   ├── controllers
│   ├── datasources
│   ├── db_models
│   ├── floor_module
│   ├── oauth
│   ├── radiomapserver
│   └── utils
├── conf
│   └── public
├── lib
├── project
├── public
│   ├── anyplace_architect
│   └── anyplace_viewer
└── web_apps
    └── anyplace_developers
```

### Notes:
1. Couchbase Views are needed for the server and API to work (will be published soon.)
2. Paremeters in conf/application.conf need to be filled according to the development or production environment.
