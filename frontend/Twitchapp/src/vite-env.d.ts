/// <reference types="vite/client" />
interface ImportMetaEnv {
    VITE_AWS_REGION: "us-east-1",
    // more env variables...
  }
  
  interface ImportMeta {
    readonly env: ImportMetaEnv
  }