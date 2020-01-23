import TestRenderer from 'react-test-renderer'

import Breadcrumbs from '..'

describe('<Breadcrumbs />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Breadcrumbs crumbs={['Bread', 'Crumb']} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
