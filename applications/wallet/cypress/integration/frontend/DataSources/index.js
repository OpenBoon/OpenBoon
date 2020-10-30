describe('DataSources', function () {
  it('can be created and deleted', function () {
    const dataSourceName = `cypress-frontend-${Date.now()}`

    cy.login()

    /**
     * Create
     */

    cy.visit(`/${this.PROJECT_ID}/data-sources/add`)

    cy.get('input[name=name]').type(dataSourceName)

    cy.contains('Source Type')
      .get('select')
      .select('Google Cloud Platform (GCP)')

    cy.get('input[name=uri]').clear().type('gs://zorroa-dev-data')

    cy.contains('Image Files').click()

    cy.get('button[type=submit]').contains('Create Data Source').click()

    cy.contains('Data source created.')

    /**
     * Delete
     */

    cy.contains(dataSourceName).parent().siblings().last().click()

    cy.contains('Delete').click()

    cy.contains('Delete Permanently').click()

    // TODO: uncomment me after this MR has been merged
    // cy.contains('Data source deleted.')

    cy.contains(dataSourceName).should('not.exist')
  })
})
