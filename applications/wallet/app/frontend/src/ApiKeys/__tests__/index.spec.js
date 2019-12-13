import TestRenderer from 'react-test-renderer'

import ApiKeys from '..'

describe('<ApiKeys />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({ pathname: '/api-keys' })

    const component = TestRenderer.create(<ApiKeys />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
