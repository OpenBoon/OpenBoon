import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'

import FaceLabeling from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = asset.id

const noop = () => () => {}

jest.mock('../TrainApply', () => 'FaceLabelingTrainApply')
jest.mock('../../Combobox', () => 'Combobox')

describe('<FaceLabeling />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<FaceLabeling />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render selected asset with no data', () => {
    require('swr').__setMockUseSWRResponse({})

    require('next/router').__setUseRouter({
      query: { assetId: ASSET_ID, projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<FaceLabeling />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render selected asset with predictions', async () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        filename: 'AssetFilename.jpg',
        predictions: [
          {
            score: 0.999,
            bbox: [0.38, 0.368, 0.484, 0.584],
            label: 'face1',
            simhash: 'MNONPMMKPLRLONLJMRLNM',
            b64Image: 'data:image/png;base64',
          },
        ],
      },
    })

    require('next/router').__setUseRouter({
      query: { assetId: ASSET_ID, projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<FaceLabeling />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('Combobox').props.onChange({ value: 'Jane' })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Cancel' })
        .props.onClick({ preventDefault: noop })
    })

    act(() => {
      component.root.findByType('Combobox').props.onChange({ value: 'Jane' })
    })

    fetch.mockRejectOnce(
      JSON.stringify({
        labels: [{ nonFieldErrors: ['Error Message'] }],
      }),
    )

    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
