{
  description = "Flake template for java project";
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem
      (system:
        let
          name = "my-app";
          pkgs = import nixpkgs {
            inherit system;
          };
          nativeBuildInputs = with pkgs; [
            maven
            jdt-language-server
          ];
          buildInputs = with pkgs; [
            jdk21
            # if you need a specific version uncomment
            # jdk8
            # jdk11
          ];
        in
        rec {
          # nix build
          packages.${name} = pkgs.buildMaven {
            inherit name;
            src = ./.;
          };
          defaultPackage = packages.${name};

          # nix run
          apps.${name} = flake-utils.lib.mkApp {
            inherit name;
            drv = packages.${name};
          };
          defaultApp = apps.${name};

          # nix develop
          devShells.default = pkgs.mkShell {
            # 👇 and now we can just inherit them
            inherit buildInputs nativeBuildInputs;

            shellHook = ''
              docker run --rm -d --name project-manager-db -p 5432:5432 -e POSTGRES_DB=dev -e POSTGRES_USER=dev -e POSTGRES_PASSWORD=dev postgres:alpine;
            '';

            APPLICATION_FORM_FILENAME="antrag_template.pdf";
            BK_ADMIN_GROUP_PREFIX="DKTK_CCP_";
            BK_ADMIN_GROUP_SUFFIX="_Verwalter";
            BK_USER_GROUP_PREFIX="DKTK_CCP_";
            BK_USER_GROUP_SUFFIX="";
            # NOTE: DEV-TORBEN is derived from the naming of the group in Keycloak
            BRIDGEHEADS_CONFIG_DEV-TORBEN_EXPLORERCODE="lens-torben-b-develop"; # NOTE: lens-torben-b-develop is the configured collection for the site in lens
            BRIDGEHEADS_CONFIG_DEV-TORBEN_FOCUSID="app1.proxy1.broker";
            BRIDGEHEADS_CONFIG_DEV-TORBEN_HUMANREADABLE="Torben Brenner Development BK";
            BRIDGEHEADS_CONFIG_DRESDEN_EXPLORERCODE="dresden"; # NOTE: lens-torben-b-develop is the configured collection for the site in lens
            BRIDGEHEADS_CONFIG_DRESDEN_FOCUSID="focus.dresden.ccp-it.dktk.dkfz.de";
            BRIDGEHEADS_CONFIG_DRESDEN_HUMANREADABLE="Torben Brenner Development BK";
            # BRIDGEHEADS_CONFIG_DAVID-J-DEVELOP_EXPLORERCODE="lens-david-j-develop";
            # BRIDGEHEADS_CONFIG_DAVID-J-DEVELOP_FOCUSID="app1.proxy1.broker";
            # BRIDGEHEADS_CONFIG_DAVID-J-DEVELOP_HUMANREADABLE="David Juárez Development BK";
            # BRIDGEHEADS_CONFIG_JURASSIC-PARK_EXPLORERCODE="lens-jurassic-park";
            # BRIDGEHEADS_CONFIG_JURASSIC-PARK_FOCUSID="app1.proxy1.broker";
            # BRIDGEHEADS_CONFIG_JURASSIC-PARK_HUMANREADABLE="Jurassic Park";
            CHECK_EXPIRED_ACTIVE_PROJECTS_CRON_EXPRESSION="0 0 0 * * *";
            DATASHIELD_TEMPLATES="opal-ccp";
            ENABLE_EMAILS="false";
            EMAIL_TEMPLATES_CONFIG="ewogICJ0ZW1wbGF0ZXMiOiB7CiAgICAiSU5WSVRBVElPTiI6IHsKICAgICAgInN1YmplY3QiOiAiQWNjZXNzIHRvIERhdGFTSElFTEQiLAogICAgICAiZmlsZXMiOiB7CiAgICAgICAgIkRFVkVMT1BFUiI6ICJkZXZlbG9wZXItaW52aXRhdGlvbi5odG1sIiwKICAgICAgICAiUElMT1QiOiAicGlsb3QtaW52aXRhdGlvbi5odG1sIiwKICAgICAgICAiRklOQUwiOiAiZmluYWwtZXhlY3V0aW9uLXVzZXItaW52aXRhdGlvbi5odG1sIiwKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LWludml0YXRpb24uaHRtbCIKICAgICAgfQogICAgfSwKICAgICJORVdfUFJPSkVDVCI6IHsKICAgICAgInN1YmplY3QiOiAiTmV3IHByb2plY3QiLAogICAgICAiZmlsZXMiOiB7CiAgICAgICAgIkJSSURHRUhFQURfQURNSU4iOiAiYnJpZGdlaGVhZC1hZG1pbi1uZXctcHJvamVjdC5odG1sIiwKICAgICAgICAiUFJPSkVDVF9NQU5BR0VSX0FETUlOIjogInByb2plY3QtbWFuYWdlci1hZG1pbi1uZXctcHJvamVjdC5odG1sIiwKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LW5ldy1wcm9qZWN0Lmh0bWwiCiAgICAgIH0KICAgIH0sCiAgICAiRklOSVNIRURfUFJPSkVDVCI6IHsKICAgICAgInN1YmplY3QiOiAiUHJvamVjdCBmaW5pc2hlZCIsCiAgICAgICJmaWxlcyI6IHsKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LXByb2plY3QtZmluaXNoZWQuaHRtbCIKICAgICAgfQogICAgfSwKICAgICJQUk9KRUNUX0JSSURHRUhFQURfQUNDRVBURUQiOiB7CiAgICAgICJzdWJqZWN0IjogIlByb2plY3QgYWNjZXB0ZWQgaW4gYnJpZGdlaGVhZCIsCiAgICAgICJmaWxlcyI6IHsKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LXByb2plY3QtYnJpZGdlaGVhZC1hY2NlcHRlZC5odG1sIgogICAgICB9CiAgICB9LAogICAgIlBST0pFQ1RfQlJJREdFSEVBRF9SRUpFQ1RFRCI6IHsKICAgICAgInN1YmplY3QiOiAiUHJvamVjdCByZWplY3RlZCBpbiBicmlkZ2VoZWFkIiwKICAgICAgImZpbGVzIjogewogICAgICAgICJERUZBVUxUIjogImRlZmF1bHQtcHJvamVjdC1icmlkZ2VoZWFkLXJlamVjdGVkLmh0bWwiCiAgICAgIH0KICAgIH0sCiAgICAiU0NSSVBUX0FDQ0VQVEVEIjogewogICAgICAic3ViamVjdCI6ICJTY3JpcHQgYWNjZXB0ZWQiLAogICAgICAiZmlsZXMiOiB7CiAgICAgICAgIkRFRkFVTFQiOiAiZGVmYXVsdC1zY3JpcHQtYWNjZXB0ZWQuaHRtbCIKICAgICAgfQogICAgfSwKICAgICJTQ1JJUFRfUkVKRUNURUQiOiB7CiAgICAgICJzdWJqZWN0IjogIlNjcmlwdCByZWplY3RlZCIsCiAgICAgICJmaWxlcyI6IHsKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LXNjcmlwdC1yZWplY3RlZC5odG1sIgogICAgICB9CiAgICB9LAogICAgIk5FV19UT0tFTl9GT1JfQVVUSEVOVElDQVRJT05fU0NSSVBUIjogewogICAgICAic3ViamVjdCI6ICJOZXcgQXV0aGVudGljYXRpb24gU2NyaXB0IGZvciBEYXRhU0hJRUxEIiwKICAgICAgImZpbGVzIjogewogICAgICAgICJERUZBVUxUIjogImRlZmF1bHQtbmV3LWF1dGhlbnRpY2F0aW9uLXNjcmlwdC5odG1sIgogICAgICB9CiAgICB9LAogICAgIlJFUVVFU1RfQ0hBTkdFU19JTl9TQ1JJUFQiOiB7CiAgICAgICJzdWJqZWN0IjogIkNoYW5nZXMgaW4gc2NyaXB0IHJlcXVlc3RlZCIsCiAgICAgICJmaWxlcyI6IHsKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LXNjcmlwdC1jaGFuZ2VzLXJlcXVlc3RlZC5odG1sIgogICAgICB9CiAgICB9LAogICAgIlJFU1VMVFNfQUNDRVBURUQiOiB7CiAgICAgICJzdWJqZWN0IjogIlJlc3VsdHMgYWNjZXB0ZWQiLAogICAgICAiZmlsZXMiOiB7CiAgICAgICAgIkRFRkFVTFQiOiAiZGVmYXVsdC1yZXN1bHRzLWFjY2VwdGVkLmh0bWwiCiAgICAgIH0KICAgIH0sCiAgICAiUkVTVUxUU19SRUpFQ1RFRCI6IHsKICAgICAgInN1YmplY3QiOiAiUmVzdWx0cyByZWplY3RlZCIsCiAgICAgICJmaWxlcyI6IHsKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LXJlc3VsdHMtcmVqZWN0ZWQuaHRtbCIKICAgICAgfQogICAgfSwKICAgICJSRVFVRVNUX0NIQU5HRVNfSU5fUFJPSkVDVCI6IHsKICAgICAgInN1YmplY3QiOiAiQ2hhbmdlcyBpbiBwcm9qZWN0IHJlcXVlc3RlZCIsCiAgICAgICJmaWxlcyI6IHsKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LXJlc3VsdHMtY2hhbmdlcy1yZXF1ZXN0ZWQuaHRtbCIKICAgICAgfQogICAgfQogIH0KfQo=";
            EMAIL_TEMPLATES_DIRECTORY="./templates";
            EXPORT_TEMPLATES="ccp,ccp-exliquid";
            FOCUS_API_KEY="App1Secret";
            FOCUS_PROJECT_MANAGER_ID="app1.proxy2.broker";
            FOCUS_URL="http://localhost:8082";
            FRONTEND_BASEURL="http://localhost:8099";
            FRONTEND_SITES_PROJECT-DASHBOARD="/project-dashboard";
            FRONTEND_SITES_PROJECT-VIEW="/project-view";
            IS_TEST_ENVIRONMENT="true";
            # KEYSTORE="C:\projects\project-manager\certs\keystore.p12";
            # KEYSTORE_PASSWORD="abc123";
            LOG_LEVEL="DEBUG";
            MANAGE_TOKENS_CRON_EXPRESSION="0 0 0 * * *";
            OIDC_CLAIM_GROUPS="groups";
            OIDC_CLIENT_ID="bridgehead-test-private";
            OIDC_CLIENT_SECRET="ERZuPn8EJrBsDgXnvtzwRo0JZlF8vBmy";
            OIDC_REALM="test-realm-01";
            OIDC_URL="https://login.verbis.dkfz.de";
            PM_ADMIN_GROUPS="CCP_Office";
            PROJECT_DOCUMENTS_DIRECTORY="./documents";
            PROJECT_MANAGER_EMAIL_FROM="no-reply@project-manager.ccp.dkfz.de";
            PUBLIC_DOCUMENTS_DIRECTORY="./public-documents";
            SECURITY_ENABLED="true";
            SMTP_HOST="localhost";
            SMTP_PASSWORD="";
            SMTP_PORT="1025";
            SMTP_USER="";
            TOKEN_MANAGER_URL="http://localhost:3030/api";
            # TRUST_STORE="C:\projects\project-manager\certs\frontend_truststore.jks";
            # TRUST_STORE_PASSWORD="abc123";
            XXXX_SPRING_PROFILES_ACTIVE="ssl";
            PROJECT_MANAGER_DB_URL="jdbc:postgresql://localhost:5432/dev";
            PROJECT_MANAGER_DB_USER="dev";
            PROJECT_MANAGER_DB_PASSWORD="dev";
            EXPLORER_URL="https://localhost";
            #
            # APPLICATION_FORM_FILENAME="antrag_template.pdf";
            # BK_ADMIN_GROUP_PREFIX="DKTK_CCP_";
            # BK_ADMIN_GROUP_SUFFIX="_Verwalter";
            # BK_USER_GROUP_PREFIX="DKTK_CCP_";
            # BK_USER_GROUP_SUFFIX="";
            # # BRIDGEHEADS_CONFIG_DAVID-J-DEVELOP_EXPLORERCODE="lens-david-j-develop";
            # # BRIDGEHEADS_CONFIG_DAVID-J-DEVELOP_FOCUSID="app1.proxy1.broker";
            # # BRIDGEHEADS_CONFIG_DAVID-J-DEVELOP_HUMANREADABLE="David Juárez Development BK";
            # # BRIDGEHEADS_CONFIG_JURASSIC-PARK_EXPLORERCODE="lens-jurassic-park";
            # # BRIDGEHEADS_CONFIG_JURASSIC-PARK_FOCUSID="app1.proxy1.broker";
            # # BRIDGEHEADS_CONFIG_JURASSIC-PARK_HUMANREADABLE="Jurassic Park";
            # DATASHIELD_TEMPLATES="opal-ccp";
            # EMAIL_TEMPLATES_CONFIG="ewogICJ0ZW1wbGF0ZXMiOiB7CiAgICAiSU5WSVRBVElPTiI6IHsKICAgICAgInN1YmplY3QiOiAiQWNjZXNzIHRvIERhdGFTSElFTEQiLAogICAgICAiZmlsZXMiOiB7CiAgICAgICAgIkRFVkVMT1BFUiI6ICJkZXZlbG9wZXItaW52aXRhdGlvbi5odG1sIiwKICAgICAgICAiUElMT1QiOiAicGlsb3QtaW52aXRhdGlvbi5odG1sIiwKICAgICAgICAiRklOQUwiOiAiZmluYWwtZXhlY3V0aW9uLXVzZXItaW52aXRhdGlvbi5odG1sIiwKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LWludml0YXRpb24uaHRtbCIKICAgICAgfQogICAgfSwKICAgICJORVdfUFJPSkVDVCI6IHsKICAgICAgInN1YmplY3QiOiAiTmV3IHByb2plY3QiLAogICAgICAiZmlsZXMiOiB7CiAgICAgICAgIkJSSURHRUhFQURfQURNSU4iOiAiYnJpZGdlaGVhZC1hZG1pbi1uZXctcHJvamVjdC5odG1sIiwKICAgICAgICAiUFJPSkVDVF9NQU5BR0VSX0FETUlOIjogInByb2plY3QtbWFuYWdlci1hZG1pbi1uZXctcHJvamVjdC5odG1sIiwKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LW5ldy1wcm9qZWN0Lmh0bWwiCiAgICAgIH0KICAgIH0sCiAgICAiRklOSVNIRURfUFJPSkVDVCI6IHsKICAgICAgInN1YmplY3QiOiAiUHJvamVjdCBmaW5pc2hlZCIsCiAgICAgICJmaWxlcyI6IHsKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LXByb2plY3QtZmluaXNoZWQuaHRtbCIKICAgICAgfQogICAgfSwKICAgICJQUk9KRUNUX0JSSURHRUhFQURfQUNDRVBURUQiOiB7CiAgICAgICJzdWJqZWN0IjogIlByb2plY3QgYWNjZXB0ZWQgaW4gYnJpZGdlaGVhZCIsCiAgICAgICJmaWxlcyI6IHsKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LXByb2plY3QtYnJpZGdlaGVhZC1hY2NlcHRlZC5odG1sIgogICAgICB9CiAgICB9LAogICAgIlBST0pFQ1RfQlJJREdFSEVBRF9SRUpFQ1RFRCI6IHsKICAgICAgInN1YmplY3QiOiAiUHJvamVjdCByZWplY3RlZCBpbiBicmlkZ2VoZWFkIiwKICAgICAgImZpbGVzIjogewogICAgICAgICJERUZBVUxUIjogImRlZmF1bHQtcHJvamVjdC1icmlkZ2VoZWFkLXJlamVjdGVkLmh0bWwiCiAgICAgIH0KICAgIH0sCiAgICAiU0NSSVBUX0FDQ0VQVEVEIjogewogICAgICAic3ViamVjdCI6ICJTY3JpcHQgYWNjZXB0ZWQiLAogICAgICAiZmlsZXMiOiB7CiAgICAgICAgIkRFRkFVTFQiOiAiZGVmYXVsdC1zY3JpcHQtYWNjZXB0ZWQuaHRtbCIKICAgICAgfQogICAgfSwKICAgICJTQ1JJUFRfUkVKRUNURUQiOiB7CiAgICAgICJzdWJqZWN0IjogIlNjcmlwdCByZWplY3RlZCIsCiAgICAgICJmaWxlcyI6IHsKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LXNjcmlwdC1yZWplY3RlZC5odG1sIgogICAgICB9CiAgICB9LAogICAgIlJFUVVFU1RfQ0hBTkdFU19JTl9TQ1JJUFQiOiB7CiAgICAgICJzdWJqZWN0IjogIkNoYW5nZXMgaW4gc2NyaXB0IHJlcXVlc3RlZCIsCiAgICAgICJmaWxlcyI6IHsKICAgICAgICAiREVGQVVMVCI6ICJkZWZhdWx0LXNjcmlwdC1jaGFuZ2VzLXJlcXVlc3RlZC5odG1sIgogICAgICB9CiAgICB9CiAgfQp9Cg==";
            # EMAIL_TEMPLATES_DIRECTORY="./templates";
            # EXPORT_TEMPLATES="ccp,ccp-exliquid";
            # FOCUS_API_KEY="App1Secret";
            # FOCUS_PROJECT_MANAGER_ID="app1.proxy2.broker";
            # FOCUS_URL="http://localhost:8082";
            # FRONTEND_BASEURL="http://localhost:8099";
            # FRONTEND_SITES_PROJECT-DASHBOARD="/project-dashboard";
            # FRONTEND_SITES_PROJECT-VIEW="/project-view";
            # IS_TEST_ENVIRONMENT="true";
            # # KEYSTORE="C:\projects\project-manager\certs\keystore.p12";
            # # KEYSTORE_PASSWORD="abc123";
            # # OIDC_CLAIM_GROUPS="groups";
            # # OIDC_CLIENT_ID="dev-torben-wsl";
            # # OIDC_CLIENT_SECRET="ek5cFLCLx2fnPy6bihRGdtJ8HJOvFL7h";
            # # OIDC_REALM="test-realm-01";
            # # OIDC_URL="https://login.verbis.dkfz.de";
            # OIDC_CLAIM_GROUPS="groups";
            # OIDC_CLIENT_ID="demo-lens-samply-de";
            # OIDC_CLIENT_SECRET="Lrzhg15e2r0k8aehf6HxK8eI8mtlxRud";
            # OIDC_REALM="master";
            # OIDC_URL="https://login.verbis.dkfz.de";
            # PM_ADMIN_GROUPS="CCP_Office";
            # PROJECT_DOCUMENTS_DIRECTORY="./documents";
            # PROJECT_MANAGER_EMAIL_FROM="no-reply@project-manager.ccp.dkfz.de";
            # PUBLIC_DOCUMENTS_DIRECTORY="./public-documents";
            # # SECURITY_ENABLED="true";
            # SMTP_HOST="localhost";
            # SMTP_PASSWORD="";
            # SMTP_PORT="1025";
            # SMTP_USER="";
            # TOKEN_MANAGER_URL="http://localhost:3030/api";
            # # TRUST_STORE="C:\projects\project-manager\certs\frontend_truststore.jks";
            # # TRUST_STORE_PASSWORD="abc123";
            # XXXX_SPRING_PROFILES_ACTIVE="ssl";
            # PROJECT_MANAGER_DB_URL="jdbc:postgresql://localhost:5432/dev";
            # PROJECT_MANAGER_DB_USER="dev";
            # PROJECT_MANAGER_DB_PASSWORD="dev";
          };
        }
      );
}
