FROM rubensa/ubuntu-tini-dev

# Link SDKMan Java candidates path to standard Linux path
RUN sudo ln -s /opt/sdkman/candidates/java/ /usr/lib/jvm

# Create vscode extensions cache directory so it is owned by the user
RUN mkdir -p /home/${USER_NAME}/.vscode-server/extensionsCache

# Create maven repository directory so it is owned by the user
RUN mkdir -p /home/${USER_NAME}/.m2/repository

