import TestRenderer from 'react-test-renderer'

import ApiKeys from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ApiKeys />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<ApiKeys />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
