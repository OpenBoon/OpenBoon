import TestRenderer from 'react-test-renderer'

import models from '../__mocks__/models'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Models from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Models />', () => {
  it('should render properly with no models', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Models />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with models', () => {
    require('next/router').__setUseRouter({
      pathname: `/[projectId]/models`,
      query: {
        projectId: PROJECT_ID,
        action: 'add-model-success',
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: models,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Models />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
