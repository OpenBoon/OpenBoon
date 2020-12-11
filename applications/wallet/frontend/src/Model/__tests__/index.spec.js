import TestRenderer from 'react-test-renderer'

import model from '../__mocks__/model'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Model from '..'

jest.mock('../../Breadcrumbs', () => 'Breadcrumbs')
jest.mock('../../ModelLabels', () => 'ModelLabels')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<Model />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
        action: 'edit-label-success',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: model })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Model />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly in edit label mode', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
        action: 'delete-label-success',
        edit: 'cat',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: model })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Model />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render removing an asset properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
        action: 'remove-asset-success',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: model })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Model />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
