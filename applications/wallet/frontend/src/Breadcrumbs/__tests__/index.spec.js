import TestRenderer from 'react-test-renderer'

import Breadcrumbs from '..'

jest.mock('../../PageTitle', () => 'PageTitle')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Breadcrumbs />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <Breadcrumbs
        crumbs={[
          { title: 'Bread', href: '/[projectId]/bread', isBeta: true },
          { title: 'Crumb', href: false, isBeta: true },
        ]}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
