import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'
import models from '../__mocks__/models'
import modelTypes from '../../ModelTypes/__mocks__/modelTypes'

import User from '../../User'

import Models from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = models.results[0].id

jest.mock('next/link', () => 'Link')
jest.mock('../Table', () => 'ModelsTable')

describe('<Models />', () => {
  it('should let the user start labeling', async () => {
    require('next/router').__setUseRouter({
      pathname: `/[projectId]/models`,
      query: {
        projectId: PROJECT_ID,
        action: 'add-model-success',
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: modelTypes })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Models />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // eslint-disable-next-line no-proto
    const spy = jest.spyOn(localStorage.__proto__, 'setItem')

    await act(async () => {
      component.root.findByProps({ children: 'Start Labeling' }).props.onClick()
    })

    expect(spy).toHaveBeenCalledWith('leftOpeningPanel', '"assetLabeling"')

    expect(spy).toHaveBeenCalledWith(
      `AssetLabelingAdd.${PROJECT_ID}`,
      `{"modelId":"${MODEL_ID}","label":"","scope":"TRAIN"}`,
    )

    spy.mockClear()
  })

  it('should let the user know a model was deleted', async () => {
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
