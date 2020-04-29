import TestRenderer from 'react-test-renderer'

import Export from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Export />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<Export />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
