import { spacing, typography } from '../Styles'

import Toggletip from '../Toggletip'

const ModelTip = () => {
  return (
    <div
      css={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        div: { marginLeft: 0 },
        button: { marginRight: 0 },
      }}
    >
      <Toggletip openToThe="right" label="Training Options">
        <div
          css={{
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            padding: spacing.base,
          }}
        >
          <h3
            css={{
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
            }}
          >
            Test Model
          </h3>
          <p>
            Test the model by running it on your dataset test assets ONLY. This
            enables you to view the results in the matrix, assess your model’s
            performance and make any adjustments before using it at scale.
          </p>
          <i>
            Once you have trained your model it will be available to use in Data
            Sources.
          </i>
        </div>
      </Toggletip>
    </div>
  )
}

export default ModelTip
