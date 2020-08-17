describe('Api Keys', function () {
  it('can be created', function () {
    const apiKeyName = `cypress-frontend-${Date.now()}`

    cy.login()

    cy.visit(`/${this.PROJECT_ID}/api-keys/add`)

    cy.get('input[name=name]').type(apiKeyName)

    cy.contains('Assets Read').click()

    cy.get('button[type=submit]').contains('Generate Key & Download').click()

    cy.contains('Key generated & copied to clipboard.')

    cy.contains('Assets Read')

    cy.visit(`/${this.PROJECT_ID}/api-keys`)

    cy.contains(apiKeyName)

    cy.contains('Assets Read')
  })

  it('can be deleted', function () {
    const apiKeyName = `cypress-frontend-${Date.now()}`

    cy.login()

    cy.fetch({
      method: 'POST',
      url: `/api/v1/projects/${this.PROJECT_ID}/api_keys/`,
      body: {
        name: apiKeyName,
        permissions: ['AssetsRead', 'AssetsDelete'],
      },
      log: false,
    })

    cy.visit(`/${this.PROJECT_ID}/api-keys`)

    cy.contains(apiKeyName).siblings().last().click()

    cy.get('button').contains('Delete').click()

    cy.contains('Delete Permanently').click()

    cy.contains(apiKeyName).should('not.exist')
  })
})
