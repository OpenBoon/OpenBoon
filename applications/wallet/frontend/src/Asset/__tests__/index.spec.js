import TestRenderer from 'react-test-renderer'

import asset from '../__mocks__/asset'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Asset from '..'

jest.mock('../../Metadata', () => 'Metadata')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Asset />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: asset })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Asset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
