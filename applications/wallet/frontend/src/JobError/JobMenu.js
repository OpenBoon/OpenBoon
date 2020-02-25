import PropTypes from 'prop-types'

import { spacing, colors } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu, { WIDTH } from '../Menu'
import Button, { VARIANTS } from '../Button'

import ChevronSvg from '../Icons/chevron.svg'

const ACTION = {
  name: 'Retry All Failures',
  action: 'retry_all_failures',
}

const JobErrorJobMenu = ({ projectId, jobId, revalidate }) => {
  const { name, action } = ACTION
  return (
    <div css={{ display: 'flex', marginBottom: -spacing.small }}>
      <Menu
        open="left"
        button={({ onBlur, onClick }) => (
          <Button
            aria-label="Toggle Error Actions Menu"
            variant={VARIANTS.SECONDARY}
            css={{
              width: WIDTH,
              paddingTop: spacing.base,
              paddingBottom: spacing.base,
              paddingLeft: spacing.normal,
              paddingRight: spacing.normal,
              flexDirection: 'row',
              justifyContent: 'space-between',
              marginBottom: spacing.small,
              color: colors.structure.black,
            }}
            onBlur={onBlur}
            onClick={onClick}
            isDisabled={false}>
            Modify Job
            <ChevronSvg width={20} />
          </Button>
        )}>
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li key={action}>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={async () => {
                    onClick()

                    await fetcher(
                      `/api/v1/projects/${projectId}/jobs/${jobId}/${action}/`,
                      { method: 'PUT' },
                    )

                    revalidate()
                  }}
                  isDisabled={false}>
                  {name}
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>
    </div>
  )
}

JobErrorJobMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  jobId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default JobErrorJobMenu
