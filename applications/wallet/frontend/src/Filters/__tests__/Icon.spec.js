import TestRenderer from 'react-test-renderer'

import FiltersIcon from '../Icon'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<FiltersIcon />', () => {
  it('should render properly with a green dot', () => {
    const filters = [
      { type: 'textContent', attribute: '', values: { query: 'Cat' } },
    ]

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        query: btoa(JSON.stringify(filters)),
      },
    })

    const component = TestRenderer.create(<FiltersIcon />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with a grey dot', () => {
    const filters = [{ type: 'textContent', attribute: '', values: {} }]

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        query: btoa(JSON.stringify(filters)),
      },
    })

    const component = TestRenderer.create(<FiltersIcon />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
