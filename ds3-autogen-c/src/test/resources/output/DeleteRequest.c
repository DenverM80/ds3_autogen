ds3_error* ds3_delete_bucket(const ds3_client* client, const ds3_request* request) {
    return _internal_request_dispatcher(client, request, NULL, NULL, NULL, NULL);
}
