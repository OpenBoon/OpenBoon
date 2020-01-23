import TestRenderer from 'react-test-renderer'

import Breadcrumbs from '..'

describe('<Breadcrumbs />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Breadcrumbs
        crumbs={[
          { title: 'Bread', href: '/bread' },
          { title: 'Crumb', href: '/bread/crumb' },
        ]}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
