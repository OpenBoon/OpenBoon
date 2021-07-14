describe('Datasets', function () {
  it('can be created and deleted', function () {
    const datasetName = `cypress-frontend-${Date.now()}`

    cy.login()

    /**
     * Create
     */

    cy.visit(`/${this.PROJECT_ID}/datasets/add`)

    cy.get('input[name=name]').type(datasetName)

    cy.contains('Classification').click()

    cy.get('button[type=submit]').contains('Create New Dataset').click()

    cy.contains('Dataset created.')

    /**
     * Delete
     */

    cy.contains(datasetName).click()

    cy.contains('Dataset Details')

    cy.get('button[aria-label="Toggle Actions Menu"]').click()

    cy.contains('Delete Dataset').click()

    cy.contains('Delete Permanently').click()

    cy.url().should(
      'eq',
      `${Cypress.config('baseUrl')}/${this.PROJECT_ID}/datasets`,
    )

    cy.contains('Dataset deleted.')

    cy.contains(datasetName).should('not.exist')
  })
})
