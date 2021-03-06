class Client(object):
    def __init__(self, endpoint, credentials, proxy=None):
        self.net_client = NetworkClient(endpoint, credentials)
        if proxy is not None:
            self.net_client = self.net_client.with_proxy(proxy)

    def get_net_client(self):
        return self.net_client

<#list clientCommands as cmd>
    ${cmd.documentation}
    def ${cmd.commandName}(self, request):
        return ${cmd.responseName}(self.net_client.get_response(request), request)
</#list>
