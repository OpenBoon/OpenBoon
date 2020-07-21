import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onSubmit } from './helpers'

const WIDTH = 300
const HEIGHT = 40

const INITIAL_STATE = {
  type: '',
  name: '',
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const ModelsAddForm = () => {
  const {
    query: { projectId },
  } = useRouter()

  const {
    data: { results: modelTypes },
  } = useSWR(`/api/v1/projects/${projectId}/models/model_types/`)

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  return (
    <Form>
      <label
        htmlFor="model-types"
        css={{ paddingBottom: spacing.base, color: colors.structure.zinc }}
      >
        Model Type
      </label>
      <div css={{ paddingTop: spacing.base, paddingBottom: spacing.base }}>
        <select
          name="model-types"
          id="model-types"
          defaultValue=""
          onChange={({ target: { value } }) => {
            dispatch({ type: value })
          }}
          css={{
            backgroundColor: colors.structure.steel,
            borderRadius: constants.borderRadius.small,
            border: 'none',
            width: WIDTH,
            height: HEIGHT,
            color: colors.structure.white,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            paddingLeft: spacing.moderate,
            MozAppearance: 'none',
            WebkitAppearance: 'none',
            backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgPHBhdGggZD0iTTE0LjI0MyA3LjU4NkwxMCAxMS44MjggNS43NTcgNy41ODYgNC4zNDMgOSAxMCAxNC42NTcgMTUuNjU3IDlsLTEuNDE0LTEuNDE0eiIgZmlsbD0iI2ZmZmZmZiIgLz4KPC9zdmc+')`,
            backgroundRepeat: `no-repeat, repeat`,
            backgroundPosition: `right ${spacing.base}px top 50%`,
            backgroundSize: constants.icons.regular,
          }}
        >
          <option value="" disabled>
            Select an option...
          </option>
          {modelTypes.map((option) => {
            return (
              <option key={option.name} value={option.name}>
                {option.name}
              </option>
            )
          })}
        </select>
      </div>

      <Input
        autoFocus
        id="name"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Name"
        type="text"
        value={state.name}
        onChange={({ target: { value } }) => dispatch({ name: value })}
        hasError={state.errors.name !== undefined}
        errorMessage={state.errors.name}
      />

      <ButtonGroup>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ dispatch, projectId, state })}
          isDisabled={!state.name || state.isLoading}
        >
          {state.isLoading ? 'Creating...' : 'Create New Model'}
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default ModelsAddForm
