import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import Modal from '../Modal'

import GearSvg from '../Icons/gear.svg'

const ApiKeysMenu = ({ projectId, apiKeyId, revalidate }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  return (
    <Menu
      open="left"
      button={({ onBlur, onClick }) => (
        <Button
          className="gear"
          aria-label="Toggle Actions Menu"
          variant={VARIANTS.NEUTRAL}
          style={{
            color: colors.structure.coal,
            padding: spacing.moderate / 2,
            borderRadius: constants.borderRadius.round,
            ':hover': { backgroundColor: colors.structure.steel },
          }}
          onBlur={onBlur}
          onClick={onClick}
          isDisabled={false}>
          <GearSvg width={20} />
        </Button>
      )}>
      {({ onClick }) => (
        <div>
          <ul>
            <li>
              <>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onClick={() => {
                    setDeleteModalOpen(true)
                  }}
                  isDisabled={false}>
                  Delete
                </Button>
                {isDeleteModalOpen && (
                  <Modal
                    onCancel={() => {
                      setDeleteModalOpen(false)
                      onClick()
                    }}
                    onConfirm={async () => {
                      setDeleteModalOpen(false)
                      onClick()

                      await fetcher(
                        `/api/v1/projects/${projectId}/apikeys/${apiKeyId}/`,
                        { method: 'DELETE' },
                      )

                      revalidate()
                    }}
                  />
                )}
              </>
            </li>
          </ul>
        </div>
      )}
    </Menu>
  )
}

ApiKeysMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  apiKeyId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ApiKeysMenu
