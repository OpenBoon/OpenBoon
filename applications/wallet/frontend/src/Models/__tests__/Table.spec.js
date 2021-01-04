import TestRenderer from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'
import models from '../__mocks__/models'
import modelTypes from '../../ModelTypes/__mocks__/modelTypes'

import User from '../../User'

import ModelsTable from '../Table'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ModelsTable />', () => {
  it('should render properly without models', async () => {
    require('next/router').__setUseRouter({
      pathname: `/[projectId]/models`,
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ModelsTable projectId={PROJECT_ID} modelTypes={modelTypes.results} />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with models', async () => {
    require('next/router').__setUseRouter({
      pathname: `/[projectId]/models`,
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: models })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ModelsTable projectId={PROJECT_ID} modelTypes={modelTypes.results} />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
