import TestRenderer from 'react-test-renderer'

import DataSourcesAdd from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../Form', () => 'DataSourcesAddForm')

describe('<DataSourcesAdd />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<DataSourcesAdd />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
