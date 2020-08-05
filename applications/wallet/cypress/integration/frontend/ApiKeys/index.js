describe('Api Keys', function () {
  it('can be created', function () {
    const now = Date.now()

    cy.login()

    cy.visit(`/${this.PROJECT_ID}/api-keys/add`)

    cy.get('input[name=name]').type(now)

    cy.contains('Assets Read').click()

    cy.get('button[type=submit]').contains('Generate Key & Download').click()

    cy.contains('Key generated & copied to clipboard.')

    cy.contains('Assets Read')

    cy.visit(`/${this.PROJECT_ID}/api-keys`)

    cy.contains(now)

    cy.contains('Assets Read')
  })
})
