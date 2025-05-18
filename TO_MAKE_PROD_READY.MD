Suggestion to make it Prod ready -

1. As we are updating the resource in transfer api , we need to make the api idempotent. So that
   caller application can make safe retries.
2. Instead of in-memory map storage , integrate with a database. To avoid Data loss on server restart. We can use Spring’s @Transactional annotation
 for atomicity and rollback on failures.
3. Publish Swagger spec for API's.
4. Secure the endpoints , so that only authenticated request is processed.
5. Error response should be  more consistent . Error response should follow a format.
6. Add alerts on getting exception on critical functionality.
7. Add more tests cases/ perform load testing on transactions mixing A→B and B→A transfers under high concurrency
8. Create CI/CD pipeline having automated builds/tests.
9. maintain audit logs of all the api's
