import TestRenderer from 'react-test-renderer'

import ApiKeysAdd from '..'

describe('<ApiKeysAdd />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({ pathname: '/api-keys/add' })

    const component = TestRenderer.create(<ApiKeysAdd />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
