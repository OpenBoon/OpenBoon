describe('Api Keys', function () {
  it('can be created and deleted', function () {
    const apiKeyName = `cypress-frontend-${Date.now()}`

    cy.login()

    /**
     * Create
     */

    cy.visit(`/${this.PROJECT_ID}/api-keys/add`)

    cy.get('input[name=name]').type(apiKeyName)

    cy.contains('Assets Read').click()

    cy.get('button[type=submit]').contains('Generate Key & Download').click()

    cy.contains('Key generated & copied to clipboard.')

    cy.contains('Assets Read')

    /**
     * Delete
     */

    cy.visit(`/${this.PROJECT_ID}/api-keys`)

    cy.contains(apiKeyName).siblings().last().click()

    cy.get('button').contains('Delete').click()

    cy.contains('Delete Permanently').click()

    cy.contains('API Key deleted.')

    cy.contains(apiKeyName).should('not.exist')
  })
})
