import TestRenderer from 'react-test-renderer'

import model from '../../Model/__mocks__/model'
import labels from '../../ModelLabels/__mocks__/modelLabels'

import ModelAssets from '..'

jest.mock('../../Breadcrumbs', () => 'Breadcrumbs')
jest.mock('../Content', () => 'ModelAssetsContent')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<ModelAssets />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]/assets',
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: { ...model, ...labels } })

    const component = TestRenderer.create(<ModelAssets moduleName="Module" />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly without labels', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]/assets',
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: { ...model } })

    const component = TestRenderer.create(<ModelAssets moduleName="Module" />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
