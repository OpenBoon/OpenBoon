describe('Api Keys', function () {
  const apiKeyName = `cypress-test-${Date.now()}`
  it('can create, read and delete an API key.', function () {
    cy.login()
    cy.apiRequest({
      method: 'POST',
      url: `/api/v1/projects/${this.PROJECT_ID}/api_keys/`,
      body: {
        name: apiKeyName,
        permissions: ['AssetsRead'],
      },
    }).then((response) => {
      expect(response.body).to.have.all.keys('accessKey', 'secretKey')
    })
    cy.request(`/api/v1/projects/${this.PROJECT_ID}/api_keys/`).then(
      (response) => {
        const { id: apiKeyId } = response.body.results.find(
          ({ name }) => name === apiKeyName,
        )
        cy.request(
          `/api/v1/projects/${this.PROJECT_ID}/api_keys/${apiKeyId}/`,
        ).then((response) => {
          expect(response.body).to.have.all.keys(
            'id',
            'name',
            'projectId',
            'accessKey',
            'permissions',
            'internal',
            'url',
          )
          expect(response.body.id).to.eq(apiKeyId)
          expect(response.body.projectId).to.eq(this.PROJECT_ID)
        })
        cy.apiRequest({
          method: 'DELETE',
          url: `/api/v1/projects/${this.PROJECT_ID}/api_keys/${apiKeyId}/`,
        })
        cy.apiRequest({
          method: 'GET',
          url: `/api/v1/projects/${this.PROJECT_ID}/api_keys/${apiKeyId}/`,
          okStatusCodes: [404],
        })
      },
    )
  })
})
