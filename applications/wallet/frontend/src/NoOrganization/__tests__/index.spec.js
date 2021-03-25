import TestRenderer from 'react-test-renderer'

import NoOrganization from '..'

describe('<NoOrganization />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<NoOrganization />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
