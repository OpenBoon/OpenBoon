describe('DataSources', function () {
  it('can be created', function () {
    const dataSourceName = `cypress-frontend-${Date.now()}`

    cy.login()

    cy.visit(`/${this.PROJECT_ID}/data-sources/add`)

    cy.get('input[name=name]').type(dataSourceName)

    cy.contains('Source Type')
      .get('select')
      .select('Google Cloud Platform (GCP)')

    cy.get('input[name=uri]').clear().type('gs://zorroa-dev-data')

    cy.contains('Image Files').click()

    cy.get('button[type=submit]').contains('Create Data Source').click()

    cy.contains('Data source created.')

    cy.contains(dataSourceName)
  })

  it('can be deleted', function () {
    const dataSourceName = `cypress-frontend-${Date.now()}`

    cy.login()

    cy.fetch({
      method: 'POST',
      url: `/api/v1/projects/${this.PROJECT_ID}/data_sources/`,
      body: {
        credentials: {},
        fileTypes: ['Images'],
        modules: [],
        name: dataSourceName,
        uri: 'gs://zorroa-dev-data',
      },
      log: false,
    })

    cy.visit(`/${this.PROJECT_ID}/data-sources/`)

    cy.contains(dataSourceName).siblings().last().click()

    cy.contains('Delete').click()

    cy.contains('Delete Permanently').click()

    cy.contains(dataSourceName).should('not.exist')
  })
})
