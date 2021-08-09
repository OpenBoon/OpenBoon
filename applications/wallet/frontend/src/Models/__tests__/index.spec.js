import TestRenderer from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'
import modelTypes from '../../ModelTypes/__mocks__/modelTypes'

import User from '../../User'

import Models from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('next/link', () => 'Link')
jest.mock('../Table', () => 'ModelsTable')

describe('<Models />', () => {
  it('should render properly', async () => {
    require('next/router').__setUseRouter({
      pathname: `/[projectId]/models`,
      query: {
        projectId: PROJECT_ID,
        action: 'delete-model-success',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: modelTypes })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Models />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
