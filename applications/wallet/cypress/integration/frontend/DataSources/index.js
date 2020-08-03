describe('DataSources', function () {
  it('can be created', function () {
    const now = Date.now()

    cy.login()

    cy.visit(`/${this.PROJECT_ID}/data-sources/add`)

    cy.get('input[name=name]').type(now)

    cy.contains('Source Type')
      .get('select')
      .select('Google Cloud Platform (GCP)')

    cy.get('input[name=uri]').clear().type('gs://zorroa-dev-data')

    cy.contains('Image Files').click()

    cy.get('button[type=submit]').contains('Create Data Source').click()

    cy.contains('Data source created.')

    cy.contains(now)
  })
})
