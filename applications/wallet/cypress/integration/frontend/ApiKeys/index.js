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

  it('can be deleted', function () {
    const now = Date.now()

    cy.login()

    cy.fetch({
      method: 'POST',
      url: `/api/v1/projects/${this.PROJECT_ID}/api_keys/`,
      body: {
        name: now,
        permissions: ['AssetsRead', 'AssetsDelete'],
      },
      log: false,
    })

    cy.visit(`/${this.PROJECT_ID}/api-keys`)

    cy.contains(now).siblings().last().click()

    cy.get('button').contains('Delete').click()

    cy.contains('Delete Permanently').click()

    cy.contains(now).should('not.exist')
  })
})
