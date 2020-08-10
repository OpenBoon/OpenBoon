describe('Api Keys', function () {
  const apiKeyName = `cypress-test-${Date.now()}`

  it('can create, read and delete an API key', function () {
    cy.login()

    cy.fetch({
      url: `/api/v1/projects/${this.PROJECT_ID}/api_keys/`,
      method: 'POST',
      body: {
        name: apiKeyName,
        permissions: ['AssetsRead'],
      },
    }).then(({ body }) => {
      expect(body).to.have.all.keys('accessKey', 'secretKey')
    })

    cy.fetch({
      url: `/api/v1/projects/${this.PROJECT_ID}/api_keys/`,
    }).then(({ body }) => {
      const { id: apiKeyId } = body.results.find(
        ({ name }) => name === apiKeyName,
      )

      cy.fetch({
        url: `/api/v1/projects/${this.PROJECT_ID}/api_keys/${apiKeyId}/`,
      }).then(({ body }) => {
        expect(body).to.have.all.keys(
          'id',
          'name',
          'projectId',
          'accessKey',
          'permissions',
          'internal',
          'url',
        )

        expect(body.id).to.eq(apiKeyId)

        expect(body.projectId).to.eq(this.PROJECT_ID)
      })

      cy.fetch({
        url: `/api/v1/projects/${this.PROJECT_ID}/api_keys/${apiKeyId}/`,
        method: 'DELETE',
      })

      cy.fetch({
        url: `/api/v1/projects/${this.PROJECT_ID}/api_keys/${apiKeyId}/`,
        okStatusCodes: [404],
      })
    })
  })
})
