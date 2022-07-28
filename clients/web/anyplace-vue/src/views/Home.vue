<template>
  <div class="home">
    <MapLeaflet/>
    <div class="container-fluid">
      <!-- TOP ROW: button-menu-left, search-box, profile-menu -->
      <div class="row">
        <div class="d-flex justify-content-between align-items-center py-4">
          <!--  btn-group: button-menu-left AND button-search IN LARGE SCREEN -->
          <!--  MAIN-MENU:     -->
          <div class="btn-group " role="group" id="mobLeftIcons">
            <div id="wrapper-menu-search" class="input-group">
              <button id="button-menu-left" class="input-group-text bg-white"><i class="fas fa-bars"></i></button>
              <!--  menu-left content: -->
              <div id="menu-left" class="menu-off menu-appearance">
                <div class="d-flex justify-content-between pt-3">
                  <h3 class="fw-bold align-self-center font-monospace">Architect</h3>
                  <button id="button-close-menu-left" class="btn-close" aria-label="Close"></button>
                </div>
                <hr class="pt-0 text-muted">
                <div class="d-flex justify-content-start pt-1 d-grid gap-2 col mx-auto">
                  <button class="btn bg-white btn-hover-color"><i class="fas fa-map-marker-alt me-3"></i>
                    Your location sharing
                  </button>
                </div>
                <div class="d-flex justify-content-start d-grid gap-2 col mx-auto">
                  <button class="btn bg-white btn-hover-color"><i class="far fa-bookmark me-3"></i>
                    Your places
                  </button>
                </div>
                <div class="d-flex justify-content-start d-grid gap-2 col mx-auto">
                  <button class="btn bg-white btn-hover-color"><i class="fas fa-route me-3"></i>
                    Your timeline
                  </button>
                </div>
                <div class="d-flex justify-content-start d-grid gap-2 col mx-auto">
                  <button class="btn bg-white btn-hover-color"><i class="far fa-star me-3"></i>
                    Favourites
                  </button>
                </div>
                <hr class="pt-0 text-muted">
                <div class="d-flex justify-content-start d-grid gap-2 col mx-auto">
                  <button class="btn bg-white btn-hover-color"><i class="fas fa-link me-3"></i>
                    Share or embed map
                  </button>
                </div>
                <div class="d-flex justify-content-start d-grid gap-2 col mx-auto">
                  <button class="btn bg-white btn-hover-color"><i class="fas fa-print me-3"></i>
                    Print
                  </button>
                </div>
                <div class="d-flex justify-content-start d-grid gap-2 col mx-auto">
                  <button class="btn bg-white btn-hover-color">
                    Edit map
                  </button>
                </div>
                <hr class="pt-0 text-muted">
                <div class="d-flex justify-content-start d-grid gap-2 col mx-auto">
                  <button class="btn bg-white btn-hover-color">
                    Tips and tricks
                  </button>
                </div>
                <div class="d-flex justify-content-start d-grid gap-2 col mx-auto">
                  <button class="btn bg-white btn-hover-color">
                    Get help
                  </button>
                </div>
                <hr class="pt-0 text-muted">
                <div class="d-flex justify-content-start d-grid gap-2 col mx-auto">
                  <button class="btn bg-white btn-hover-color">
                    Language
                    <i class="fas fa-language"></i></button>
                </div>
                <div class="d-flex justify-content-start d-grid gap-2 col mx-auto">
                  <button class="btn bg-white btn-hover-color">
                    Maps activity
                  </button>
                </div>
              </div> <!--End of content main menu-->

              <!--SEARCH INPUT:-->
              <input id="input-search" type="text" class="form-control border-sides-none d-none d-lg-block"
                     placeholder="Search" aria-label="Search">
              <button id="button-search" class="input-group-text bg-white">
                <i class="fas fa-search"></i>
              </button>
            </div>
          </div>

          <!-- PROFILE - MENU-->
          <div class="profile-menu">
            <button class="btn btn-secondary circle-icon" type="button" id="profile-button"
                    data-bs-toggle="modal"
                    data-bs-target="#modal-user-account">
              <i class="fas fa-user fa-2x"></i>
            </button>
          </div>
        </div>
      </div>
    </div><!-- end of container-fluid-->

    <!--   ALIGN-BUTTONS-ON-BOTTOM : align circle buttons on right-end of page -->
    <div class="container-fluid">
      <div class="row align-buttons-on-bottom">
        <div class="col">
          <div id="wrapper-actions" class="btn-group-vertical d-none" role="group">
            <button id="button1" class="btn mb-3 btn-light circle-icon" type="button">
              <i class="fas fa-map-marked-alt fa-2x"></i>
            </button>
            <button class="btn btn-light circle-icon" type="button" id="button2">
              <i class="fas fa-map-marker-alt fa-2x"></i>
            </button>
            <button class="btn btn-light mb-3 circle-icon my-2" type="button" id="button3">
              <i class="fas fa-route fa-2x"></i>
            </button>
            <button class="btn btn-light circle-icon" type="button" id="button4">
              <i class="fas fa-directions fa-2x"></i>
            </button>
          </div>
          <div class="d-flex justify-content-end my-2">
            <button id="button-toggle-actions" class="btn btn-primary circle-icon" type="button">
              <i class="fas fa-angle-double-up fa-2x"></i>
            </button>
          </div>
        </div>
      </div>
    </div>  <!-- end of container-fluid -->

    <!-- MODALS -->
    <!-- PROFILE-MODAL -->
    <div class="modal-user-profile modal fade right" id="modal-user-account" aria-labelledby="exampleModalLabel"
         aria-hidden="true">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <i class="fas fa-user fa-2x pe-3"></i>
            <h5 class="modal-title" id="exampleModalLabel">Alan Turing</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <button class="dropdown-item" type="button">Profile</button>
            <a href="settings" class="dropdown-item" type="button">Settings</a>
          </div>
          <div class="modal-footer">
            <a href="about" class="dropdown-item" type="button">About</a>
            <button class="dropdown-item" type="button">Terms of Service</button>
            <button class="dropdown-item" type="button">Privacy</button>
            <small class="text-muted mx-auto" style="font-size: 8px">Anyplace &copy; 2021</small>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script lang="ts">
import {Options, Vue} from 'vue-class-component';
import MapLeaflet from "@/components/MapLeaflet.vue"; // @ is an alias to /src

@Options({
  name: "Anyplace",
  components: {
    MapLeaflet,
  },
  mounted() {
        enableUiWithoutVue() // TODO break it up and do it with vue
  },
  data() {
        return { }
  },
  methods: {
  }
})

export default class Home extends Vue { }

// TODO with vue/components/etc..
function enableUiWithoutVue(): void {
  console.log("enableUiWithoutVue")

  const SCREEN_SIZE_MEDIUM = 1007

  // UNIQUE ELEMENTS:
  /** search box. on mobile is hidden when not in use. */
  const elInputSearch: HTMLInputElement = document.getElementById('input-search') as HTMLInputElement
  /** bars button that shows the left menu ({@link elMenuLeft}) */
  const elButtonOpenMenuLeft: HTMLButtonElement = document.getElementById('button-menu-left') as HTMLButtonElement
  /** Left full-height menu for app-specific options (e.g. anyplace architect) */
  const elMenuLeft: HTMLDivElement = document.getElementById('menu-left') as HTMLDivElement
  /** button that closes {@link elMenuLeft} */
  const elButtonCloseMenuLeft: HTMLButtonElement = document.getElementById('button-close-menu-left') as HTMLButtonElement
  /** When clicked is submits a search query */
  const elButtonSearch: HTMLButtonElement = document.getElementById('button-search') as HTMLButtonElement

  /** Wrapper of the **top-left** menu-search component that consists of:
   *    - bars-menu button {@link elButtonOpenMenuLeft},
   *    - search input {@link elInputSearch}, and
   *    - search button {@link elButtonSearch}
   */
  const elWrapperMenuAndSearch: HTMLDivElement = document.getElementById('wrapper-menu-search') as HTMLDivElement

  /** Wrapper for the bottom-right buttons */
  const elWrapperActions: HTMLDivElement = document.getElementById('wrapper-actions') as HTMLDivElement
  /** Toggles the bottom-right action buttons */
  const elButtonToggleActions: HTMLButtonElement = document.getElementById('button-toggle-actions') as HTMLButtonElement

  function isScreenMediumOrSmall() {
    return document.body.clientWidth < SCREEN_SIZE_MEDIUM
  }

  function isHiddenInputSearchOnMobile() {
    return elInputSearch.classList.contains("d-none")
  }

  function showInputSearchOnMobile() {
    return elInputSearch.classList.remove("d-none")
  }

  function hideInputSearchOnMobile() {
    return elInputSearch.classList.add("d-none")
  }

  // SEARCH: on enter
  elInputSearch.addEventListener("keyup", function (event) {
    if (event.key === 'Enter') {
      trySearch()
    }
  });

  /** SEARCH: on {@link elButtonSearch} press */
  elButtonSearch.addEventListener("click", function () {
    if (isScreenMediumOrSmall()) {
      if (isHiddenInputSearchOnMobile()) {
        showInputSearchOnMobile();
        elInputSearch.focus()
      } else {
        trySearch();
        // hideInputSearchOnMobile() // optional..
      }
    } else { // large screens
      trySearch();
    }
  });

  // TOGGLE LEFT MENU (app-specific menu)
  elButtonOpenMenuLeft.addEventListener("click", function (event) {
    elMenuLeft.style.display = "block";
  });
  elButtonCloseMenuLeft.addEventListener("click", function (event) {
    elMenuLeft.style.display = "none";
  });


  /** When loosing focus from the wrapper, or its children:
   * - {@link elButtonOpenMenuLeft }
   * - {@link elInputSearch}
   * - {@link elButtonSearch }
   */
  elWrapperMenuAndSearch.addEventListener('focusout', (function (event) {
    // when the event concerns any other object than the child of the wrapper
    if (!this.contains((event.relatedTarget) as HTMLElement)) {
      hideInputSearchOnMobile()
    }
  }));

  /** If {@link elInputSearch } has any text, then perform a query. */
  function trySearch() {
    if (elInputSearch.value) {
      // TODO: implement query
      console.log("query:'" + elInputSearch.value + "'")
    }
  }

  /** When clicked on {@link elButtonToggleActions }, show/hidden {@link elWrapperActions }  */
  elButtonToggleActions.addEventListener("click", function () {
    if (elWrapperActions.classList.contains("d-none")) {  // if hidden show
      elWrapperActions.classList.remove("d-none")
    } else { // hide
      elWrapperActions.classList.add("d-none");
    }
  });

  const myModalEl: HTMLElement = document.getElementById('modal-user-account') as HTMLElement;
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  myModalEl.addEventListener('hidden.bs.modal', function () {
    // keyboard: false
  })

}
</script>
