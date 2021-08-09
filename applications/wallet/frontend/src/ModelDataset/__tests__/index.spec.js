import TestRenderer from 'react-test-renderer'

import model from '../../Model/__mocks__/model'

import ModelDataset from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

jest.mock('../../DatasetConcepts', () => 'DatasetConcepts')
jest.mock('../../ModelLink', () => 'ModelLink')
jest.mock('../Header', () => 'ModelDatasetHeader')

const noop = () => () => {}

describe('<ModelDataset />', () => {
  it('should render properly without a datasetId', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    const component = TestRenderer.create(
      <ModelDataset model={model} setErrors={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with a datasetId', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    const component = TestRenderer.create(
      <ModelDataset
        model={{ ...model, datasetId: DATASET_ID }}
        setErrors={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not render when model requires a file upload', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    const component = TestRenderer.create(
      <ModelDataset
        model={{ ...model, datasetId: DATASET_ID, state: 'RequiresUpload' }}
        setErrors={noop}
      />,
    )

    expect(component.toJSON()).toEqual(null)
  })
})
